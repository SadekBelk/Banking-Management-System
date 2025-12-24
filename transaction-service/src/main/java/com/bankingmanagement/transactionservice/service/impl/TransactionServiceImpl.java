package com.bankingmanagement.transactionservice.service.impl;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.exception.InvalidAmountException;
import com.bankingmanagement.transactionservice.exception.InvalidStatusTransitionException;
import com.bankingmanagement.transactionservice.exception.InvalidTransactionTypeException;
import com.bankingmanagement.transactionservice.exception.SameAccountTransferException;
import com.bankingmanagement.transactionservice.exception.TransactionException;
import com.bankingmanagement.transactionservice.exception.TransactionNotFoundException;
import com.bankingmanagement.transactionservice.mapper.TransactionMapper;
import com.bankingmanagement.transactionservice.model.Transaction;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.model.TransactionType;
import com.bankingmanagement.transactionservice.repository.TransactionRepository;
import com.bankingmanagement.transactionservice.service.TransactionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    // TODO: Inject AccountService gRPC client for balance checks
    // private final AccountServiceGrpcClient accountServiceClient;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper
            // AccountServiceGrpcClient accountServiceClient
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        // this.accountServiceClient = accountServiceClient;
    }

    /* =========================
       CORE TRANSACTION OPERATIONS
       ========================= */

    @Override
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        log.info("Creating transaction: type={}, from={}, to={}, amount={}",
                requestDto.getTransactionType(),
                requestDto.getFromAccountId(),
                requestDto.getToAccountId(),
                requestDto.getAmount());

        // 1️⃣ Validate request
        validateTransactionRequest(requestDto);

        // 2️⃣ Create transaction entity with PENDING status
        Transaction transaction = transactionMapper.toEntity(requestDto);
        transaction.setStatus(TransactionStatus.PENDING);

        // 3️⃣ Save transaction (generates ID for idempotency key)
        transaction = transactionRepository.save(transaction); // Saved as PENDING
        log.info("Transaction created with ID: {}", transaction.getId());

        // 4️⃣ Process based on transaction type
        try {
            processTransaction(transaction); // Currently does nothing (TODO: gRPC)
            transaction.setStatus(TransactionStatus.COMPLETED); // ← Immediately set to COMPLETED
            log.info("Transaction {} completed successfully", transaction.getId());
        } catch (TransactionException ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("Transaction {} failed: {}", transaction.getId(), ex.getMessage());
            throw ex; // Re-throw after status update
        }

        // 5️⃣ Save final status and return
        transaction = transactionRepository.save(transaction); // Saved as COMPLETED
        return transactionMapper.toResponseDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(Long id) {
        log.debug("Fetching transaction by ID: {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + id));

        return transactionMapper.toResponseDto(transaction);
    }

    /* =========================
       QUERY OPERATIONS
       ========================= */

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByAccountId(Long accountId) {
        log.debug("Fetching all transactions for account: {}", accountId);

        // Combine outgoing and incoming transactions
        List<Transaction> outgoing = transactionRepository.findByFromAccountId(accountId);
        List<Transaction> incoming = transactionRepository.findByToAccountId(accountId);

        // Merge and sort by createdAt (most recent first)
        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.addAll(outgoing);
        allTransactions.addAll(incoming);

        return allTransactions.stream()
                .distinct() // Remove duplicates (same account transfers)
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getOutgoingTransactions(Long accountId) {
        log.debug("Fetching outgoing transactions for account: {}", accountId);

        return transactionRepository.findByFromAccountId(accountId).stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getIncomingTransactions(Long accountId) {
        log.debug("Fetching incoming transactions for account: {}", accountId);

        return transactionRepository.findByToAccountId(accountId).stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByStatus(TransactionStatus status) {
        log.debug("Fetching transactions by status: {}", status);

        return transactionRepository.findByStatus(status).stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactions() {
        log.debug("Fetching all transactions");

        return transactionRepository.findAll().stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /* =========================
       STATUS MANAGEMENT
       ========================= */

    @Override
    public TransactionResponseDto updateTransactionStatus(Long transactionId, TransactionStatus newStatus) {
        log.info("Updating transaction {} status to {}", transactionId, newStatus);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));

        // Validate status transition
        validateStatusTransition(transaction.getStatus(), newStatus);

        transaction.setStatus(newStatus);
        transaction = transactionRepository.save(transaction);

        log.info("Transaction {} status updated to {}", transactionId, newStatus);
        return transactionMapper.toResponseDto(transaction);
    }

    /* =========================
       PRIVATE HELPER METHODS
       ========================= */

    /**
     * Validates the transaction request before processing.
     */
    private void validateTransactionRequest(TransactionRequestDto request) {
        // Rule 1: Amount must be positive
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Transaction amount must be positive");
        }

        // Rule 2: Validate accounts based on transaction type
        TransactionType type = request.getTransactionType();

        switch (type) {
            case TRANSFER:
                // Both accounts required, must be different
                if (request.getFromAccountId() == null || request.getToAccountId() == null) {
                    throw new InvalidTransactionTypeException("Transfer requires both source and destination accounts");
                }
                if (request.getFromAccountId().equals(request.getToAccountId())) {
                    throw new SameAccountTransferException("Cannot transfer to the same account");
                }
                break;

            case WITHDRAWAL:
                // Only source account required
                if (request.getFromAccountId() == null) {
                    throw new InvalidTransactionTypeException("Withdrawal requires a source account");
                }
                break;

            case DEPOSIT:
                // Only destination account required
                if (request.getToAccountId() == null) {
                    throw new InvalidTransactionTypeException("Deposit requires a destination account");
                }
                break;

            default:
                throw new InvalidTransactionTypeException("Unknown transaction type: " + type);
        }

        // Rule 3: Minimum transaction amount (business rule example)
        BigDecimal minimumAmount = new BigDecimal("0.01");
        if (request.getAmount().compareTo(minimumAmount) < 0) {
            throw new InvalidAmountException("Minimum transaction amount is " + minimumAmount);
        }

        // Rule 4: Maximum transaction amount (daily limit example)
        BigDecimal maximumAmount = new BigDecimal("1000000.00");
        if (request.getAmount().compareTo(maximumAmount) > 0) {
            throw new InvalidAmountException("Maximum transaction amount is " + maximumAmount);
        }
    }

    /**
     * Processes the transaction based on its type.
     * This is where gRPC calls to AccountService would happen.
     */
    private void processTransaction(Transaction transaction) {
        log.debug("Processing transaction: {}", transaction.getId());

        switch (transaction.getTransactionType()) {
            case TRANSFER:
                processTransfer(transaction);
                break;

            case DEPOSIT:
                processDeposit(transaction);
                break;

            case WITHDRAWAL:
                processWithdrawal(transaction);
                break;

            default:
                throw new InvalidTransactionTypeException("Unsupported transaction type");
        }
    }

    /**
     * Processes a transfer between two accounts.
     * 
     * Steps:
     * 1. Verify source account exists and has sufficient balance (gRPC)
     * 2. Verify destination account exists (gRPC)
     * 3. Reserve balance from source account (gRPC)
     * 4. Credit destination account (gRPC)
     * 5. Confirm debit from source account (gRPC)
     */
    private void processTransfer(Transaction transaction) {
        log.debug("Processing TRANSFER: {} -> {}, amount={}",
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount());

        // TODO: Implement gRPC calls to AccountService
        // 
        // Step 1: Get source account balance
        // GetBalanceResponse sourceBalance = accountServiceClient.getBalance(
        //     GetBalanceRequest.newBuilder()
        //         .setAccountId(transaction.getFromAccountId().toString())
        //         .build());
        //
        // Step 2: Check sufficient funds
        // BigDecimal available = new BigDecimal(sourceBalance.getAvailableBalance().getAmount());
        // if (available.compareTo(transaction.getAmount()) < 0) {
        //     throw new TransactionException("Insufficient funds");
        // }
        //
        // Step 3: Reserve balance (with idempotency)
        // ReserveBalanceResponse reservation = accountServiceClient.reserveBalance(
        //     ReserveBalanceRequest.newBuilder()
        //         .setAccountId(transaction.getFromAccountId().toString())
        //         .setAmount(Money.newBuilder()
        //             .setAmount(transaction.getAmount().toPlainString())
        //             .setCurrency("USD")
        //             .build())
        //         .setTransactionId(transaction.getId().toString())
        //         .build());
        //
        // if (!reservation.getSuccess()) {
        //     throw new TransactionException("Failed to reserve balance: " + 
        //         reservation.getError().getMessage());
        // }

        log.info("TRANSFER processed successfully (gRPC integration pending)");
    }

    /**
     * Processes a deposit to an account.
     */
    private void processDeposit(Transaction transaction) {
        log.debug("Processing DEPOSIT: account={}, amount={}",
                transaction.getToAccountId(),
                transaction.getAmount());

        // TODO: Implement gRPC call to credit account
        // This would typically come from an external source (ATM, bank transfer, etc.)

        log.info("DEPOSIT processed successfully (gRPC integration pending)");
    }

    /**
     * Processes a withdrawal from an account.
     */
    private void processWithdrawal(Transaction transaction) {
        log.debug("Processing WITHDRAWAL: account={}, amount={}",
                transaction.getFromAccountId(),
                transaction.getAmount());

        // TODO: Implement gRPC calls similar to transfer
        // 1. Check balance
        // 2. Reserve funds
        // 3. Confirm withdrawal

        log.info("WITHDRAWAL processed successfully (gRPC integration pending)");
    }

    /**
     * Validates that a status transition is allowed.
     * 
     * Allowed transitions:
     * - PENDING -> COMPLETED
     * - PENDING -> FAILED
     * - FAILED -> PENDING (retry)
     */
    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        // Cannot change COMPLETED transactions
        if (currentStatus == TransactionStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                "Cannot change status of completed transaction");
        }

        // Cannot go from FAILED to COMPLETED directly
        if (currentStatus == TransactionStatus.FAILED && newStatus == TransactionStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                "Cannot mark failed transaction as completed. Retry the transaction instead.");
        }

        log.debug("Status transition validated: {} -> {}", currentStatus, newStatus);
    }
}

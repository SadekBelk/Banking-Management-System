package com.bankingmanagement.transactionservice.service.impl;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.exception.InvalidStatusTransitionException;
import com.bankingmanagement.transactionservice.exception.TransactionNotFoundException;
import com.bankingmanagement.transactionservice.mapper.TransactionMapper;
import com.bankingmanagement.transactionservice.model.Transaction;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.repository.TransactionRepository;
import com.bankingmanagement.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transaction Service Implementation.
 * 
 * Note: Most transaction operations are done via gRPC (TransactionGrpcService).
 * This service provides REST API support and internal operations.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /* =========================
       CORE TRANSACTION OPERATIONS
       ========================= */

    @Override
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        log.info("Creating transaction via REST: type={}, from={}, to={}, amount={}",
                requestDto.getType(),
                requestDto.getSourceAccountId(),
                requestDto.getDestinationAccountId(),
                requestDto.getAmount());

        // Create transaction entity
        Transaction transaction = transactionMapper.toEntity(requestDto);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setStatus(TransactionStatus.PENDING);
        
        // Generate idempotency key if not provided
        if (transaction.getIdempotencyKey() == null || transaction.getIdempotencyKey().isBlank()) {
            transaction.setIdempotencyKey("REST-" + UUID.randomUUID());
        }

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, referenceNumber={}", 
                transaction.getId(), transaction.getReferenceNumber());

        return transactionMapper.toResponseDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(UUID id) {
        log.debug("Fetching transaction by ID: {}", id);
        Transaction transaction = findTransactionById(id);
        return transactionMapper.toResponseDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReferenceNumber(String referenceNumber) {
        log.debug("Fetching transaction by reference: {}", referenceNumber);
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with reference: " + referenceNumber));
        return transactionMapper.toResponseDto(transaction);
    }

    /* =========================
       QUERY OPERATIONS
       ========================= */

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByAccountId(UUID accountId) {
        log.debug("Fetching all transactions for account: {}", accountId);

        List<Transaction> outgoing = transactionRepository.findBySourceAccountId(accountId);
        List<Transaction> incoming = transactionRepository.findByDestinationAccountId(accountId);

        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.addAll(outgoing);
        allTransactions.addAll(incoming);

        return allTransactions.stream()
                .distinct()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getOutgoingTransactions(UUID accountId) {
        log.debug("Fetching outgoing transactions for account: {}", accountId);
        return transactionRepository.findBySourceAccountId(accountId).stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getIncomingTransactions(UUID accountId) {
        log.debug("Fetching incoming transactions for account: {}", accountId);
        return transactionRepository.findByDestinationAccountId(accountId).stream()
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
    public TransactionResponseDto updateTransactionStatus(UUID transactionId, TransactionStatus newStatus) {
        log.info("Updating transaction {} status to {}", transactionId, newStatus);

        Transaction transaction = findTransactionById(transactionId);
        validateStatusTransition(transaction.getStatus(), newStatus);

        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(Instant.now());
        
        if (newStatus == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(Instant.now());
        }

        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} status updated to {}", transactionId, newStatus);
        
        return transactionMapper.toResponseDto(transaction);
    }

    @Override
    public TransactionResponseDto failTransaction(UUID transactionId, String reason) {
        log.info("Failing transaction {}: {}", transactionId, reason);

        Transaction transaction = findTransactionById(transactionId);
        validateStatusTransition(transaction.getStatus(), TransactionStatus.FAILED);

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transaction.setUpdatedAt(Instant.now());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} marked as FAILED", transactionId);
        
        return transactionMapper.toResponseDto(transaction);
    }

    /* =========================
       PRIVATE HELPERS
       ========================= */

    private Transaction findTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + id));
    }

    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        if (currentStatus == TransactionStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Cannot change status of completed transaction");
        }
        if (currentStatus == TransactionStatus.FAILED && newStatus == TransactionStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Cannot mark failed transaction as completed");
        }
    }
}

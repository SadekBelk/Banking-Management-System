package com.bankingmanagement.transactionservice.service;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.TransactionStatus;

import java.util.List;

/**
 * Service interface for managing banking transactions.
 * Handles transfers, deposits, and withdrawals with proper business rules.
 */
public interface TransactionService {

    /* =========================
       CORE TRANSACTION OPERATIONS
       ========================= */

    /**
     * Creates and processes a new transaction.
     * 
     * Business Rules:
     * - Validates source and destination accounts exist (via gRPC to AccountService)
     * - Checks sufficient balance for TRANSFER and WITHDRAWAL
     * - Reserves balance before processing (idempotent)
     * - Updates transaction status based on outcome
     * 
     * @param requestDto the transaction request containing amount, accounts, and type
     * @return the created transaction with status
     */
    TransactionResponseDto createTransaction(TransactionRequestDto requestDto);

    /**
     * Retrieves a transaction by its unique ID.
     * 
     * @param id the transaction ID
     * @return the transaction details
     * @throws com.bankingmanagement.transactionservice.exception.TransactionNotFoundException if not found
     */
    TransactionResponseDto getTransactionById(Long id);

    /* =========================
       QUERY OPERATIONS
       ========================= */

    /**
     * Retrieves all transactions for a given account (both sent and received).
     * Useful for account statement generation.
     * 
     * @param accountId the account ID
     * @return list of transactions involving this account
     */
    List<TransactionResponseDto> getTransactionsByAccountId(Long accountId);

    /**
     * Retrieves all outgoing transactions from a specific account.
     * 
     * @param accountId the source account ID
     * @return list of transactions where this account is the sender
     */
    List<TransactionResponseDto> getOutgoingTransactions(Long accountId);

    /**
     * Retrieves all incoming transactions to a specific account.
     * 
     * @param accountId the destination account ID
     * @return list of transactions where this account is the receiver
     */
    List<TransactionResponseDto> getIncomingTransactions(Long accountId);

    /**
     * Retrieves all transactions with a specific status.
     * Useful for monitoring pending or failed transactions.
     * 
     * @param status the transaction status (PENDING, COMPLETED, FAILED)
     * @return list of transactions with the given status
     */
    List<TransactionResponseDto> getTransactionsByStatus(TransactionStatus status);

    /**
     * Retrieves all transactions in the system.
     * Should be paginated in production for large datasets.
     * 
     * @return list of all transactions
     */
    List<TransactionResponseDto> getAllTransactions();

    /* =========================
       STATUS MANAGEMENT
       ========================= */

    /**
     * Updates the status of a transaction.
     * Used internally after processing or for manual intervention.
     * 
     * Business Rules:
     * - Only certain status transitions are allowed
     * - COMPLETED transactions cannot be changed
     * 
     * @param transactionId the transaction ID
     * @param newStatus the new status to set
     * @return the updated transaction
     */
    TransactionResponseDto updateTransactionStatus(Long transactionId, TransactionStatus newStatus);
}

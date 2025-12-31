package com.bankingmanagement.transactionservice.service;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.TransactionStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing banking transactions.
 * 
 * Note: Most transaction operations are done via gRPC (TransactionGrpcService).
 * This interface provides REST API support and internal service layer operations.
 */
public interface TransactionService {

    /* =========================
       CORE TRANSACTION OPERATIONS
       ========================= */

    /**
     * Creates a new transaction (REST API entry point).
     * For gRPC operations, use TransactionGrpcService directly.
     */
    TransactionResponseDto createTransaction(TransactionRequestDto requestDto);

    /**
     * Retrieves a transaction by its unique ID.
     */
    TransactionResponseDto getTransactionById(UUID id);

    /**
     * Retrieves a transaction by reference number.
     */
    TransactionResponseDto getTransactionByReferenceNumber(String referenceNumber);

    /* =========================
       QUERY OPERATIONS
       ========================= */

    /**
     * Retrieves all transactions for a given account (both sent and received).
     */
    List<TransactionResponseDto> getTransactionsByAccountId(UUID accountId);

    /**
     * Retrieves all outgoing transactions from a specific account.
     */
    List<TransactionResponseDto> getOutgoingTransactions(UUID accountId);

    /**
     * Retrieves all incoming transactions to a specific account.
     */
    List<TransactionResponseDto> getIncomingTransactions(UUID accountId);

    /**
     * Retrieves all transactions with a specific status.
     */
    List<TransactionResponseDto> getTransactionsByStatus(TransactionStatus status);

    /**
     * Retrieves all transactions.
     */
    List<TransactionResponseDto> getAllTransactions();

    /* =========================
       STATUS MANAGEMENT
       ========================= */

    /**
     * Updates the status of a transaction.
     */
    TransactionResponseDto updateTransactionStatus(UUID transactionId, TransactionStatus newStatus);

    /**
     * Marks a transaction as failed with a reason.
     */
    TransactionResponseDto failTransaction(UUID transactionId, String reason);
}

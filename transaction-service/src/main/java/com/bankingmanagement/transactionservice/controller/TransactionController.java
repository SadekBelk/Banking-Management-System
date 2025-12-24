package com.bankingmanagement.transactionservice.controller;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "API for managing banking TRANSACTIONS")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /* =========================
       TRANSACTION OPERATIONS
       ========================= */

    /**
     * Create a new transaction (transfer, deposit, or withdrawal).
     * POST /api/transactions
     */
    @PostMapping
    @Operation(summary = "Create transaction", description = "Creates and processes a new transaction")
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionRequestDto request
    ) {
        log.info("Creating transaction: type={}, from={}, to={}",
                request.getTransactionType(),
                request.getFromAccountId(),
                request.getToAccountId());

        TransactionResponseDto response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a specific transaction by ID.
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a transaction by its unique ID")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        log.debug("Fetching transaction by ID: {}", id);
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    /**
     * Get all transactions in the system.
     * GET /api/transactions
     */
    @GetMapping
    @Operation(summary = "Get all transactions", description = "Retrieves all transactions (use for admin/reporting)")
    public ResponseEntity<List<TransactionResponseDto>> getAllTransactions() {
        log.debug("Fetching all transactions");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /* =========================
       ACCOUNT-SPECIFIC QUERIES
       ========================= */

    /**
     * Get all transactions for a specific account (both sent and received).
     * GET /api/transactions/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account", 
               description = "Retrieves all transactions involving a specific account (sent + received)")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByAccount(
            @PathVariable Long accountId
    ) {
        log.debug("Fetching transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
    }

    /**
     * Get outgoing transactions from a specific account.
     * GET /api/transactions/account/{accountId}/outgoing
     */
    @GetMapping("/account/{accountId}/outgoing")
    @Operation(summary = "Get outgoing transactions", 
               description = "Retrieves transactions sent from a specific account")
    public ResponseEntity<List<TransactionResponseDto>> getOutgoingTransactions(
            @PathVariable Long accountId
    ) {
        log.debug("Fetching outgoing transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getOutgoingTransactions(accountId));
    }

    /**
     * Get incoming transactions to a specific account.
     * GET /api/transactions/account/{accountId}/incoming
     */
    @GetMapping("/account/{accountId}/incoming")
    @Operation(summary = "Get incoming transactions", 
               description = "Retrieves transactions received by a specific account")
    public ResponseEntity<List<TransactionResponseDto>> getIncomingTransactions(
            @PathVariable Long accountId
    ) {
        log.debug("Fetching incoming transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getIncomingTransactions(accountId));
    }

    /* =========================
       STATUS-BASED QUERIES
       ========================= */

    /**
     * Get transactions by status (PENDING, COMPLETED, FAILED).
     * GET /api/transactions/status/{status}
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get transactions by status", 
               description = "Retrieves transactions filtered by status (PENDING, COMPLETED, FAILED)")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByStatus(
            @PathVariable TransactionStatus status
    ) {
        log.debug("Fetching transactions with status: {}", status);
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    /* =========================
       STATUS MANAGEMENT
       ========================= */

    /**
     * Update transaction status (admin operation).
     * PATCH /api/transactions/{id}/status
     * 
     * Request body: { "status": "COMPLETED" | "FAILED" | "PENDING" }
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update transaction status", 
               description = "Updates the status of a transaction (admin operation)")
    public ResponseEntity<TransactionResponseDto> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status
    ) {
        log.info("Updating transaction {} status to {}", id, status);
        TransactionResponseDto response = transactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(response);
    }
}

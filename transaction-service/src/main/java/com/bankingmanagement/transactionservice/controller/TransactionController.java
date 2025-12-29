package com.bankingmanagement.transactionservice.controller;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "API for managing banking TRANSACTIONS")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /* =========================
       TRANSACTION OPERATIONS
       ========================= */

    @PostMapping
    @Operation(summary = "Create transaction", description = "Creates a new transaction record")
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionRequestDto request
    ) {
        log.info("Creating transaction: type={}, from={}, to={}",
                request.getType(),
                request.getSourceAccountId(),
                request.getDestinationAccountId());

        TransactionResponseDto response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a transaction by its unique ID")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable UUID id) {
        log.debug("Fetching transaction by ID: {}", id);
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get transaction by reference", description = "Retrieves a transaction by reference number")
    public ResponseEntity<TransactionResponseDto> getTransactionByReference(
            @PathVariable String referenceNumber
    ) {
        log.debug("Fetching transaction by reference: {}", referenceNumber);
        return ResponseEntity.ok(transactionService.getTransactionByReferenceNumber(referenceNumber));
    }

    @GetMapping
    @Operation(summary = "Get all transactions", description = "Retrieves all transactions")
    public ResponseEntity<List<TransactionResponseDto>> getAllTransactions() {
        log.debug("Fetching all transactions");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /* =========================
       ACCOUNT-SPECIFIC QUERIES
       ========================= */

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account", 
               description = "Retrieves all transactions involving a specific account")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByAccount(
            @PathVariable UUID accountId
    ) {
        log.debug("Fetching transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
    }

    @GetMapping("/account/{accountId}/outgoing")
    @Operation(summary = "Get outgoing transactions", 
               description = "Retrieves transactions sent from a specific account")
    public ResponseEntity<List<TransactionResponseDto>> getOutgoingTransactions(
            @PathVariable UUID accountId
    ) {
        log.debug("Fetching outgoing transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getOutgoingTransactions(accountId));
    }

    @GetMapping("/account/{accountId}/incoming")
    @Operation(summary = "Get incoming transactions", 
               description = "Retrieves transactions received by a specific account")
    public ResponseEntity<List<TransactionResponseDto>> getIncomingTransactions(
            @PathVariable UUID accountId
    ) {
        log.debug("Fetching incoming transactions for account: {}", accountId);
        return ResponseEntity.ok(transactionService.getIncomingTransactions(accountId));
    }

    /* =========================
       STATUS-BASED QUERIES
       ========================= */

    @GetMapping("/status/{status}")
    @Operation(summary = "Get transactions by status", 
               description = "Retrieves transactions filtered by status")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByStatus(
            @PathVariable TransactionStatus status
    ) {
        log.debug("Fetching transactions with status: {}", status);
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    /* =========================
       STATUS MANAGEMENT
       ========================= */

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update transaction status", description = "Updates the status of a transaction")
    public ResponseEntity<TransactionResponseDto> updateTransactionStatus(
            @PathVariable UUID id,
            @RequestParam TransactionStatus status
    ) {
        log.info("Updating transaction {} status to {}", id, status);
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, status));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark transaction as failed", description = "Marks a transaction as failed with a reason")
    public ResponseEntity<TransactionResponseDto> failTransaction(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        log.info("Failing transaction {}: {}", id, reason);
        return ResponseEntity.ok(transactionService.failTransaction(id, reason));
    }
}

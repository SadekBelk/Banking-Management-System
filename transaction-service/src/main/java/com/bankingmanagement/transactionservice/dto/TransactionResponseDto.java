package com.bankingmanagement.transactionservice.dto;

import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for transaction data.
 * 
 * Maps from Transaction entity for REST API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDto {

    private UUID id;
    private String referenceNumber;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private UUID paymentId;
    private UUID reservationId;
    private String description;
    private String idempotencyKey;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}

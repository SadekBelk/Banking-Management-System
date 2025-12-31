package com.bankingmanagement.transactionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transaction entity - represents a ledger entry in the banking system.
 * 
 * This is the immutable audit trail for all money movements.
 * Once created, the core fields (accounts, amount) should never change.
 * Only status can be updated (PENDING → COMPLETED or FAILED).
 */
@Data
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_source_account", columnList = "sourceAccountId"),
    @Index(name = "idx_transaction_dest_account", columnList = "destinationAccountId"),
    @Index(name = "idx_transaction_payment_id", columnList = "paymentId"),
    @Index(name = "idx_transaction_idempotency_key", columnList = "idempotencyKey", unique = true)
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /** Human-readable reference number (e.g., TXN-A1B2C3D4) */
    @Column(nullable = false, unique = true, length = 20)
    private String referenceNumber;
    
    /** Source account UUID (from account-service) */
    @Column(nullable = false)
    private UUID sourceAccountId;
    
    /** Destination account UUID (from account-service) */
    @Column(nullable = false)
    private UUID destinationAccountId;
    
    /** Transaction amount */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    /** Currency code (e.g., USD, EUR) */
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    
    /** Reference to payment-service payment ID */
    @Column
    private UUID paymentId;
    
    /** Reference to account-service reservation ID */
    @Column
    private UUID reservationId;
    
    /** Optional description */
    @Column(length = 500)
    private String description;
    
    /** Idempotency key to prevent duplicate transactions */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    
    /** Reason for failure (only set if status is FAILED) */
    @Column(length = 500)
    private String failureReason;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column
    private Instant updatedAt;
    
    @Column
    private Instant completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

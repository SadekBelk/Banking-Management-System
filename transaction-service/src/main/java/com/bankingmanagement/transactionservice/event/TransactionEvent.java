package com.bankingmanagement.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Base class for transaction-related Kafka events.
 * 
 * All transaction events share common fields but may have additional
 * event-specific data. The eventType field determines the actual event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {
    
    /**
     * Event metadata
     */
    private String eventId;           // Unique event identifier (UUID)
    private String eventType;         // TRANSACTION_CREATED, TRANSACTION_COMPLETED, TRANSACTION_FAILED
    private Instant eventTimestamp;   // When the event occurred
    private String eventVersion;      // Schema version for evolution (e.g., "1.0")
    
    /**
     * Transaction data
     */
    private String transactionId;     // UUID of the transaction
    private String referenceNumber;   // Human-readable reference (TXN-XXXXX)
    private String sourceAccountId;   // Source account UUID
    private String destinationAccountId; // Destination account UUID
    private BigDecimal amount;        // Transaction amount
    private String currency;          // Currency code (USD, EUR, etc.)
    private String transactionType;   // TRANSFER, DEPOSIT, WITHDRAWAL, etc.
    private String transactionStatus; // PENDING, COMPLETED, FAILED
    
    /**
     * Related entities
     */
    private String paymentId;         // Reference to payment-service payment
    private String reservationId;     // Reference to account-service reservation
    
    /**
     * Additional context
     */
    private String description;       // Transaction description
    private String failureReason;     // Only set for FAILED events
    
    /**
     * Event type constants
     */
    public static final String TRANSACTION_CREATED = "TRANSACTION_CREATED";
    public static final String TRANSACTION_COMPLETED = "TRANSACTION_COMPLETED";
    public static final String TRANSACTION_FAILED = "TRANSACTION_FAILED";
}

package com.bankingmanagement.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Base class for payment-related Kafka events.
 * 
 * All payment events share common fields but may have additional
 * event-specific data. The eventType field determines the actual event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    
    /**
     * Event metadata
     */
    private String eventId;           // Unique event identifier (UUID)
    private String eventType;         // PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED
    private Instant eventTimestamp;   // When the event occurred
    private String eventVersion;      // Schema version for evolution (e.g., "1.0")
    
    /**
     * Payment data
     */
    private String paymentId;         // UUID of the payment
    private String referenceNumber;   // Human-readable reference (PAY-XXXXX)
    private String sourceAccountId;   // Source account UUID
    private String destinationAccountId; // Destination account UUID
    private BigDecimal amount;        // Payment amount
    private String currency;          // Currency code (USD, EUR, etc.)
    private String paymentType;       // TRANSFER, BILL_PAYMENT, P2P, etc.
    private String paymentStatus;     // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    
    /**
     * Related entities
     */
    private String reservationId;     // Reference to account-service reservation
    private String transactionId;     // Reference to transaction-service transaction
    private String idempotencyKey;    // Idempotency key for the payment
    
    /**
     * Additional context
     */
    private String description;       // Payment description
    private String failureReason;     // Only set for FAILED events
    
    /**
     * Timestamps
     */
    private Instant createdAt;        // When the payment was created
    private Instant processedAt;      // When the payment was processed (completed/failed)
    
    /**
     * Event type constants
     */
    public static final String PAYMENT_INITIATED = "PAYMENT_INITIATED";
    public static final String PAYMENT_PROCESSING = "PAYMENT_PROCESSING";
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
}

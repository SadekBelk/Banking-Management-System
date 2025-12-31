package com.bankingmanagement.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_source_account", columnList = "source_account_id"),
                @Index(name = "idx_payment_destination_account", columnList = "destination_account_id"),
                @Index(name = "idx_payment_reference", columnList = "reference_number"),
                @Index(name = "idx_payment_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "reference_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "reference_number", nullable = false, updatable = false)
    private String referenceNumber;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 500)
    private String description;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * The balance reservation ID from account-service.
     * This links the payment to the reserved funds.
     * Set during payment processing, used for commit/rollback.
     */
    @Column(name = "reservation_id")
    private String reservationId;

    /**
     * The transaction ID from transaction-service.
     * This links the payment to the ledger entry.
     */
    @Column(name = "transaction_id")
    private String transactionId;

    /**
     * Idempotency key for the balance reservation.
     * Prevents duplicate reservations on retry.
     */
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}

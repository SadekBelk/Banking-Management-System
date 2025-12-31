package com.bankingmanagement.accountservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a temporary hold/reservation on an account's balance.
 * 
 * This follows the Authorization → Clearing → Settlement pattern used in real banking:
 * - PENDING: Money is reserved (locked) but not deducted
 * - COMMITTED: Reservation converted to actual deduction
 * - RELEASED: Reservation cancelled, money unlocked
 * - EXPIRED: Reservation timed out and was auto-released
 */
@Entity
@Table(
        name = "balance_reservations",
        indexes = {
                @Index(name = "idx_reservation_account_id", columnList = "account_id"),
                @Index(name = "idx_reservation_idempotency_key", columnList = "idempotency_key"),
                @Index(name = "idx_reservation_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_idempotency_key", columnNames = "idempotency_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceReservation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    /**
     * Idempotency key provided by payment-service.
     * Ensures duplicate reservation requests return the same reservation.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    /**
     * Transaction ID linking this reservation to the ledger entry.
     * Set when CommitReservation is called.
     */
    @Column(name = "transaction_id")
    private String transactionId;

    /**
     * Reason for release (if released/failed).
     * Useful for debugging and audit trails.
     */
    @Column(name = "release_reason")
    private String releaseReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    /**
     * Expiration time for the reservation.
     * If not committed by this time, the reservation can be auto-released.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;
}

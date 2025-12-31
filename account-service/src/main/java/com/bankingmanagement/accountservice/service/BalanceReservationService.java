package com.bankingmanagement.accountservice.service;

import com.bankingmanagement.accountservice.exception.AccountNotFoundException;
import com.bankingmanagement.accountservice.exception.InsufficientBalanceException;
import com.bankingmanagement.accountservice.exception.InvalidReservationStateException;
import com.bankingmanagement.accountservice.exception.ReservationNotFoundException;
import com.bankingmanagement.accountservice.model.BalanceReservation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for managing balance reservations.
 * 
 * This service owns the reservation lifecycle:
 * - Reserve: Lock money (PENDING)
 * - Commit: Deduct money permanently (COMMITTED)  
 * - Release: Unlock money (RELEASED)
 * 
 * Key principle: Reservations are internal to account-service.
 * The gRPC layer calls this service, never the repository directly.
 */
public interface BalanceReservationService {

    /**
     * Get the available balance for an account.
     * Available = Actual Balance - Total Pending Reservations
     * 
     * @param accountId The account ID
     * @return Available balance (actual - reserved)
     */
    BigDecimal getAvailableBalance(UUID accountId);

    /**
     * Reserve (lock) money in an account.
     * 
     * This does NOT deduct money - it only prevents it from being used elsewhere.
     * Idempotent: Same idempotencyKey returns existing reservation.
     * 
     * @param accountId Account to reserve from
     * @param amount Amount to reserve
     * @param currency Currency code
     * @param idempotencyKey Unique key from payment-service
     * @return The created or existing reservation
     * @throws InsufficientBalanceException if available balance is insufficient
     * @throws AccountNotFoundException if account doesn't exist
     */
    BalanceReservation reserveBalance(UUID accountId, BigDecimal amount, String currency, String idempotencyKey);

    /**
     * Commit a reservation - permanently deduct the money.
     * 
     * This is called AFTER the transaction ledger entry exists.
     * The transactionId links the deduction to the audit trail.
     * 
     * @param reservationId The reservation to commit
     * @param transactionId The transaction ID from transaction-service
     * @throws ReservationNotFoundException if reservation doesn't exist
     * @throws InvalidReservationStateException if reservation is not PENDING
     */
    void commitReservation(UUID reservationId, String transactionId);

    /**
     * Release a reservation - unlock the money without deducting.
     * 
     * Called when payment fails or times out.
     * 
     * @param reservationId The reservation to release
     * @param reason Reason for release (for audit)
     * @throws ReservationNotFoundException if reservation doesn't exist
     * @throws InvalidReservationStateException if reservation is not PENDING
     */
    void releaseReservation(UUID reservationId, String reason);

    /**
     * Find an existing reservation by ID.
     * 
     * @param reservationId The reservation ID
     * @return The reservation
     * @throws ReservationNotFoundException if not found
     */
    BalanceReservation getReservation(UUID reservationId);
}

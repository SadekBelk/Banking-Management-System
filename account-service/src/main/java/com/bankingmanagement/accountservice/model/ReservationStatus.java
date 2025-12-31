package com.bankingmanagement.accountservice.model;

/**
 * Status of a balance reservation.
 * 
 * Lifecycle:
 * PENDING → COMMITTED (success path)
 * PENDING → RELEASED (explicit cancellation)
 * PENDING → EXPIRED (timeout/auto-release)
 */
public enum ReservationStatus {
    
    /**
     * Money is reserved/locked but not yet deducted.
     * The account's available balance is reduced, but actual balance is unchanged.
     */
    PENDING,
    
    /**
     * Reservation has been committed - money was actually deducted.
     * This is the terminal success state.
     */
    COMMITTED,
    
    /**
     * Reservation was explicitly released/cancelled.
     * Money is unlocked and available again.
     */
    RELEASED,
    
    /**
     * Reservation expired due to timeout.
     * Money was auto-released back to the account.
     */
    EXPIRED
}

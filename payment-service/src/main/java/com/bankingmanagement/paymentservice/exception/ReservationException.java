package com.bankingmanagement.paymentservice.exception;

/**
 * Exception thrown when a reservation operation fails.
 * This can happen due to:
 * - Reservation not found
 * - Invalid reservation state (e.g., trying to commit an already released reservation)
 * - Reservation expired
 */
public class ReservationException extends RuntimeException {

    public ReservationException(String message) {
        super(message);
    }

    public ReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}

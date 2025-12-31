package com.bankingmanagement.accountservice.exception;

import com.bankingmanagement.accountservice.model.ReservationStatus;

import java.util.UUID;

/**
 * Thrown when attempting an invalid state transition on a reservation.
 * e.g., trying to commit an already released reservation.
 */
public class InvalidReservationStateException extends RuntimeException {

    private final UUID reservationId;
    private final ReservationStatus currentStatus;
    private final String attemptedOperation;

    public InvalidReservationStateException(UUID reservationId, ReservationStatus currentStatus, String attemptedOperation) {
        super(String.format(
                "Cannot %s reservation %s. Current status: %s",
                attemptedOperation, reservationId, currentStatus
        ));
        this.reservationId = reservationId;
        this.currentStatus = currentStatus;
        this.attemptedOperation = attemptedOperation;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public ReservationStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedOperation() {
        return attemptedOperation;
    }
}

package com.bankingmanagement.accountservice.exception;

import java.util.UUID;

/**
 * Thrown when a reservation is not found.
 */
public class ReservationNotFoundException extends RuntimeException {

    private final UUID reservationId;

    public ReservationNotFoundException(UUID reservationId) {
        super("Reservation not found with id: " + reservationId);
        this.reservationId = reservationId;
    }

    public ReservationNotFoundException(String message) {
        super(message);
        this.reservationId = null;
    }

    public UUID getReservationId() {
        return reservationId;
    }
}

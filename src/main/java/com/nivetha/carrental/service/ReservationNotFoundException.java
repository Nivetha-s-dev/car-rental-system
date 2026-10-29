package com.nivetha.carrental.service;

import java.util.UUID;

/** Raised when a cancellation references a reservation that does not exist. */
public final class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID reservationId) {
        super("Reservation not found: " + reservationId);
    }
}

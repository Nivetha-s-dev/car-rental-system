package com.nivetha.carrental.domain;

import java.util.Objects;
import java.util.UUID;

/** A confirmed reservation tying one car to one rental period. */
public record Reservation(UUID id, Car car, RentalPeriod period) {

    public Reservation {
        Objects.requireNonNull(id, "Reservation id must not be null");
        Objects.requireNonNull(car, "Car must not be null");
        Objects.requireNonNull(period, "Rental period must not be null");
    }
}

package com.nivetha.carrental.domain;

import java.util.Objects;

/** A uniquely identifiable car in the fleet. */
public record Car(String id, CarType type) {

    public Car {
        Objects.requireNonNull(id, "Car id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Car id must not be blank");
        }
        Objects.requireNonNull(type, "Car type must not be null");
    }
}

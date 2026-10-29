package com.nivetha.carrental.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainObjectsTest {

    @Test
    @DisplayName("Provides the three required car types and readable names")
    void providesRequiredCarTypes() {
        assertAll(
                () -> assertEquals("Sedan", CarType.SEDAN.displayName()),
                () -> assertEquals("SUV", CarType.SUV.displayName()),
                () -> assertEquals("Van", CarType.VAN.displayName())
        );
    }

    @Test
    @DisplayName("Validates car identity and type")
    void validatesCar() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new Car(null, CarType.SEDAN)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Car(" ", CarType.SEDAN)),
                () -> assertThrows(NullPointerException.class, () -> new Car("SEDAN-1", null))
        );
    }

    @Test
    @DisplayName("Validates every reservation component")
    void validatesReservation() {
        UUID id = UUID.randomUUID();
        Car car = new Car("SEDAN-1", CarType.SEDAN);
        RentalPeriod period = new RentalPeriod(LocalDateTime.of(2026, 11, 10, 9, 0), 1);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new Reservation(null, car, period)),
                () -> assertThrows(NullPointerException.class, () -> new Reservation(id, null, period)),
                () -> assertThrows(NullPointerException.class, () -> new Reservation(id, car, null))
        );
    }
}

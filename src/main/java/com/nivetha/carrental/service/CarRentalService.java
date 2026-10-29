package com.nivetha.carrental.service;

import com.nivetha.carrental.domain.CarType;
import com.nivetha.carrental.domain.RentalPeriod;
import com.nivetha.carrental.domain.Reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public application boundary for reserving and managing rental cars. */
public interface CarRentalService {

    Reservation reserve(CarType type, RentalPeriod period);

    default Reservation reserve(CarType type, LocalDateTime start, int days) {
        return reserve(type, new RentalPeriod(start, days));
    }

    void cancel(UUID reservationId);

    Optional<Reservation> findReservation(UUID reservationId);

    int availableCount(CarType type, RentalPeriod period);

    /** Returns an immutable snapshot of all current reservations. */
    List<Reservation> getReservations();
}

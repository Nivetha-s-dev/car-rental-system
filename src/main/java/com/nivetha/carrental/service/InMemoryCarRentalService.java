package com.nivetha.carrental.service;

import com.nivetha.carrental.domain.Car;
import com.nivetha.carrental.domain.CarType;
import com.nivetha.carrental.domain.RentalPeriod;
import com.nivetha.carrental.domain.Reservation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe, in-memory implementation intended for this self-contained assessment.
 * The synchronized write makes selecting and saving a reservation one atomic operation.
 */
public final class InMemoryCarRentalService implements CarRentalService {

    private final Map<CarType, List<Car>> carsByType;
    private final Map<UUID, Reservation> reservationsById = new HashMap<>();

    public InMemoryCarRentalService(Map<CarType, Integer> inventoryByType) {
        Objects.requireNonNull(inventoryByType, "Inventory must not be null");
        carsByType = createFleet(inventoryByType);
    }

    @Override
    public synchronized Reservation reserve(CarType type, RentalPeriod period) {
        requireTypeAndPeriod(type, period);

        Car selectedCar = carsByType.get(type).stream()
                .filter(car -> isAvailable(car, period))
                .findFirst()
                .orElseThrow(() -> new NoCarAvailableException(type, period));

        Reservation reservation = new Reservation(UUID.randomUUID(), selectedCar, period);
        reservationsById.put(reservation.id(), reservation);
        return reservation;
    }

    @Override
    public synchronized void cancel(UUID reservationId) {
        Objects.requireNonNull(reservationId, "Reservation id must not be null");
        Reservation removed = reservationsById.remove(reservationId);
        if (removed == null) {
            throw new ReservationNotFoundException(reservationId);
        }
    }

    @Override
    public synchronized Optional<Reservation> findReservation(UUID reservationId) {
        Objects.requireNonNull(reservationId, "Reservation id must not be null");
        return Optional.ofNullable(reservationsById.get(reservationId));
    }

    @Override
    public synchronized int availableCount(CarType type, RentalPeriod period) {
        requireTypeAndPeriod(type, period);
        return (int) carsByType.get(type).stream()
                .filter(car -> isAvailable(car, period))
                .count();
    }

    @Override
    public synchronized List<Reservation> getReservations() {
        return reservationsById.values().stream()
                .sorted(Comparator.comparing((Reservation r) -> r.period().start())
                        .thenComparing(r -> r.car().id())
                        .thenComparing(r -> r.id()))
                .toList();
    }

    private boolean isAvailable(Car car, RentalPeriod requestedPeriod) {
        return reservationsById.values().stream()
                .filter(reservation -> reservation.car().equals(car))
                .noneMatch(reservation -> reservation.period().overlaps(requestedPeriod));
    }

    private static void requireTypeAndPeriod(CarType type, RentalPeriod period) {
        Objects.requireNonNull(type, "Car type must not be null");
        Objects.requireNonNull(period, "Rental period must not be null");
    }

    private static Map<CarType, List<Car>> createFleet(Map<CarType, Integer> inventoryByType) {
        Map<CarType, List<Car>> fleet = new EnumMap<>(CarType.class);

        for (CarType type : CarType.values()) {
            Integer count = inventoryByType.get(type);
            Objects.requireNonNull(count, "Missing inventory count for " + type);
            if (count < 0) {
                throw new IllegalArgumentException("Inventory count must not be negative for " + type);
            }

            List<Car> cars = new ArrayList<>(count);
            for (int sequence = 1; sequence <= count; sequence++) {
                cars.add(new Car(type.name() + "-" + sequence, type));
            }
            fleet.put(type, List.copyOf(cars));
        }

        return Map.copyOf(fleet);
    }
}

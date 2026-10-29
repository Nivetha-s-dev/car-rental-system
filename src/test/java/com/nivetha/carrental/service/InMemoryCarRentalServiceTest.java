package com.nivetha.carrental.service;

import com.nivetha.carrental.domain.CarType;
import com.nivetha.carrental.domain.RentalPeriod;
import com.nivetha.carrental.domain.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCarRentalServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Test
    @DisplayName("Reserves the requested car type for the requested date/time and days")
    void reservesRequestedCarTypeAndPeriod() {
        CarRentalService service = serviceWithInventory(1, 1, 1);
        RentalPeriod period = new RentalPeriod(START, 2);

        Reservation reservation = service.reserve(CarType.SEDAN, START, 2);

        assertAll(
                () -> assertEquals(CarType.SEDAN, reservation.car().type()),
                () -> assertEquals("SEDAN-1", reservation.car().id()),
                () -> assertEquals(period, reservation.period()),
                () -> assertEquals(reservation, service.findReservation(reservation.id()).orElseThrow()),
                () -> assertEquals(0, service.availableCount(CarType.SEDAN, period))
        );
    }

    @Test
    @DisplayName("Supports sedan, SUV, and van reservations independently")
    void supportsAllRequiredCarTypes() {
        CarRentalService service = serviceWithInventory(1, 1, 1);
        RentalPeriod period = new RentalPeriod(START, 1);

        Reservation sedan = service.reserve(CarType.SEDAN, period);
        Reservation suv = service.reserve(CarType.SUV, period);
        Reservation van = service.reserve(CarType.VAN, period);

        assertAll(
                () -> assertEquals(CarType.SEDAN, sedan.car().type()),
                () -> assertEquals(CarType.SUV, suv.car().type()),
                () -> assertEquals(CarType.VAN, van.car().type()),
                () -> assertEquals(3, service.getReservations().size())
        );
    }

    @Test
    @DisplayName("Never exceeds the limited inventory for an overlapping period")
    void enforcesInventoryLimit() {
        CarRentalService service = serviceWithInventory(2, 1, 1);
        RentalPeriod period = new RentalPeriod(START, 3);

        Reservation first = service.reserve(CarType.SEDAN, period);
        Reservation second = service.reserve(CarType.SEDAN, period);

        assertAll(
                () -> assertNotEquals(first.car().id(), second.car().id()),
                () -> assertEquals(0, service.availableCount(CarType.SEDAN, period)),
                () -> assertThrows(NoCarAvailableException.class,
                        () -> service.reserve(CarType.SEDAN, period)),
                () -> assertEquals(2, service.getReservations().size()),
                () -> assertEquals(1, service.availableCount(CarType.SUV, period))
        );
    }

    @Test
    @DisplayName("Assigns another car for a partial overlap instead of double-booking")
    void handlesPartialOverlap() {
        CarRentalService service = serviceWithInventory(2, 0, 0);
        Reservation first = service.reserve(CarType.SEDAN, new RentalPeriod(START, 3));

        Reservation second = service.reserve(CarType.SEDAN, new RentalPeriod(START.plusDays(1), 1));

        assertAll(
                () -> assertNotEquals(first.car().id(), second.car().id()),
                () -> assertThrows(NoCarAvailableException.class,
                        () -> service.reserve(CarType.SEDAN, new RentalPeriod(START.plusDays(1), 1)))
        );
    }

    @Test
    @DisplayName("Reuses a car for a back-to-back reservation at the exclusive end")
    void allowsBackToBackReservation() {
        CarRentalService service = serviceWithInventory(1, 0, 0);
        Reservation first = service.reserve(CarType.SEDAN, new RentalPeriod(START, 2));

        Reservation next = service.reserve(CarType.SEDAN, new RentalPeriod(START.plusDays(2), 1));

        assertAll(
                () -> assertEquals(first.car().id(), next.car().id()),
                () -> assertEquals(2, service.getReservations().size())
        );
    }

    @Test
    @DisplayName("Reuses a car when reservations are separated by unused time")
    void allowsSeparatedReservations() {
        CarRentalService service = serviceWithInventory(1, 0, 0);
        Reservation first = service.reserve(CarType.SEDAN, new RentalPeriod(START, 1));

        Reservation later = service.reserve(CarType.SEDAN, new RentalPeriod(START.plusDays(5), 2));

        assertEquals(first.car().id(), later.car().id());
    }

    @Test
    @DisplayName("Cancellation releases exactly the cancelled car for a new reservation")
    void cancellationReleasesInventory() {
        CarRentalService service = serviceWithInventory(1, 0, 0);
        RentalPeriod period = new RentalPeriod(START, 2);
        Reservation original = service.reserve(CarType.SEDAN, period);

        service.cancel(original.id());
        Reservation replacement = service.reserve(CarType.SEDAN, period);

        assertAll(
                () -> assertTrue(service.findReservation(original.id()).isEmpty()),
                () -> assertEquals(original.car().id(), replacement.car().id()),
                () -> assertEquals(1, service.getReservations().size())
        );
    }

    @Test
    @DisplayName("Rejects unknown and repeated cancellations")
    void rejectsUnknownCancellation() {
        CarRentalService service = serviceWithInventory(1, 0, 0);
        Reservation reservation = service.reserve(CarType.SEDAN, START, 1);
        service.cancel(reservation.id());

        assertAll(
                () -> assertThrows(ReservationNotFoundException.class,
                        () -> service.cancel(reservation.id())),
                () -> assertThrows(ReservationNotFoundException.class,
                        () -> service.cancel(UUID.randomUUID()))
        );
    }

    @Test
    @DisplayName("Returns a chronologically sorted, immutable reservation snapshot")
    void returnsImmutableSortedSnapshot() {
        CarRentalService service = serviceWithInventory(2, 0, 0);
        Reservation later = service.reserve(CarType.SEDAN, new RentalPeriod(START.plusDays(5), 1));
        Reservation earlier = service.reserve(CarType.SEDAN, new RentalPeriod(START, 1));

        List<Reservation> snapshot = service.getReservations();

        assertAll(
                () -> assertEquals(List.of(earlier, later), snapshot),
                () -> assertThrows(UnsupportedOperationException.class, snapshot::clear),
                () -> assertEquals(2, service.getReservations().size())
        );
    }

    @Test
    @DisplayName("Validates reservation, availability, lookup, and cancellation arguments")
    void validatesMethodArguments() {
        CarRentalService service = serviceWithInventory(1, 1, 1);
        RentalPeriod period = new RentalPeriod(START, 1);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> service.reserve(null, period)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.reserve(CarType.SEDAN, (RentalPeriod) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.reserve(CarType.SEDAN, (LocalDateTime) null, 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.reserve(CarType.SEDAN, START, 0)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.availableCount(null, period)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.availableCount(CarType.SEDAN, null)),
                () -> assertThrows(NullPointerException.class, () -> service.findReservation(null)),
                () -> assertThrows(NullPointerException.class, () -> service.cancel(null)),
                () -> assertTrue(service.getReservations().isEmpty())
        );
    }

    @Test
    @DisplayName("Rejects incomplete or invalid fleet configuration")
    void validatesInventoryConfiguration() {
        Map<CarType, Integer> missingVan = new EnumMap<>(CarType.class);
        missingVan.put(CarType.SEDAN, 1);
        missingVan.put(CarType.SUV, 1);

        Map<CarType, Integer> negativeSuv = inventoryMap(1, -1, 1);
        Map<CarType, Integer> nullVan = new HashMap<>();
        nullVan.put(CarType.SEDAN, 1);
        nullVan.put(CarType.SUV, 1);
        nullVan.put(CarType.VAN, null);

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new InMemoryCarRentalService(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new InMemoryCarRentalService(missingVan)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InMemoryCarRentalService(negativeSuv)),
                () -> assertThrows(NullPointerException.class,
                        () -> new InMemoryCarRentalService(nullVan))
        );
    }

    @Test
    @DisplayName("Supports a zero-count category without allowing a reservation")
    void supportsZeroInventory() {
        CarRentalService service = serviceWithInventory(0, 1, 1);
        RentalPeriod period = new RentalPeriod(START, 1);

        assertAll(
                () -> assertEquals(0, service.availableCount(CarType.SEDAN, period)),
                () -> assertThrows(NoCarAvailableException.class,
                        () -> service.reserve(CarType.SEDAN, period)),
                () -> assertEquals(1, service.availableCount(CarType.SUV, period))
        );
    }

    @Test
    @DisplayName("Concurrent requests cannot oversell the last car")
    void preventsConcurrentOverselling() throws Exception {
        CarRentalService service = serviceWithInventory(1, 0, 0);
        RentalPeriod period = new RentalPeriod(START, 2);
        int requestCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    try {
                        service.reserve(CarType.SEDAN, period);
                        return "RESERVED";
                    } catch (NoCarAvailableException exception) {
                        return "FULL";
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            long reserved = 0;
            long full = 0;
            for (Future<String> future : futures) {
                String outcome = future.get(5, TimeUnit.SECONDS);
                if (outcome.equals("RESERVED")) {
                    reserved++;
                } else {
                    full++;
                }
            }

            assertEquals(1, reserved);
            assertEquals(requestCount - 1, full);
            assertEquals(1, service.getReservations().size());
            assertEquals(0, service.availableCount(CarType.SEDAN, period));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static CarRentalService serviceWithInventory(int sedans, int suvs, int vans) {
        return new InMemoryCarRentalService(inventoryMap(sedans, suvs, vans));
    }

    private static Map<CarType, Integer> inventoryMap(int sedans, int suvs, int vans) {
        Map<CarType, Integer> inventory = new EnumMap<>(CarType.class);
        inventory.put(CarType.SEDAN, sedans);
        inventory.put(CarType.SUV, suvs);
        inventory.put(CarType.VAN, vans);
        return inventory;
    }
}

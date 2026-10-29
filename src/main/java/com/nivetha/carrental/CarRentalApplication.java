package com.nivetha.carrental;

import com.nivetha.carrental.domain.CarType;
import com.nivetha.carrental.domain.RentalPeriod;
import com.nivetha.carrental.domain.Reservation;
import com.nivetha.carrental.service.CarRentalService;
import com.nivetha.carrental.service.InMemoryCarRentalService;
import com.nivetha.carrental.service.NoCarAvailableException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/** Runnable example that demonstrates successful booking, capacity limits, and cancellation. */
public final class CarRentalApplication {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CarRentalApplication() {
    }

    public static void main(String[] args) {
        Map<CarType, Integer> inventory = new EnumMap<>(CarType.class);
        inventory.put(CarType.SEDAN, 2);
        inventory.put(CarType.SUV, 1);
        inventory.put(CarType.VAN, 1);

        CarRentalService service = new InMemoryCarRentalService(inventory);
        LocalDateTime start = LocalDate.now().plusDays(7).atTime(10, 0);
        RentalPeriod requestedPeriod = new RentalPeriod(start, 3);

        System.out.println("=== Car Rental Reservation Demo ===");
        System.out.println("Requested period: " + describe(requestedPeriod));
        System.out.println("Initial availability: " + availability(service, requestedPeriod));

        Reservation firstSedan = service.reserve(CarType.SEDAN, start, 3);
        Reservation secondSedan = service.reserve(CarType.SEDAN, start, 3);
        service.reserve(CarType.SUV, start, 2);
        service.reserve(CarType.VAN, start, 1);

        System.out.println("Reserved: " + describe(firstSedan));
        System.out.println("Reserved: " + describe(secondSedan));
        System.out.println("Availability after bookings: " + availability(service, requestedPeriod));

        try {
            service.reserve(CarType.SEDAN, start, 3);
        } catch (NoCarAvailableException exception) {
            System.out.println("Expected capacity error: " + exception.getMessage());
        }

        service.cancel(secondSedan.id());
        System.out.println("Cancelled reservation: " + secondSedan.id());
        System.out.println("Availability after cancellation: " + availability(service, requestedPeriod));

        Reservation replacement = service.reserve(CarType.SEDAN, start, 3);
        System.out.println("Reserved released car: " + describe(replacement));
        System.out.println("Current reservation count: " + service.getReservations().size());
    }

    private static String availability(CarRentalService service, RentalPeriod period) {
        return "sedan=" + service.availableCount(CarType.SEDAN, period)
                + ", SUV=" + service.availableCount(CarType.SUV, period)
                + ", van=" + service.availableCount(CarType.VAN, period);
    }

    private static String describe(Reservation reservation) {
        return reservation.car().id() + " [" + reservation.id() + "] for "
                + describe(reservation.period());
    }

    private static String describe(RentalPeriod period) {
        return DATE_TIME.format(period.start()) + " for " + period.days()
                + " day(s), ends " + DATE_TIME.format(period.endExclusive()) + " (exclusive)";
    }
}

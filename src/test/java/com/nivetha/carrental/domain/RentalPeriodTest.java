package com.nivetha.carrental.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalPeriodTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 10, 9, 30);

    @Test
    @DisplayName("Calculates an exclusive end from the start date/time and number of days")
    void calculatesExclusiveEnd() {
        RentalPeriod period = new RentalPeriod(START, 3);

        assertAll(
                () -> assertEquals(START, period.start()),
                () -> assertEquals(3, period.days()),
                () -> assertEquals(START.plusDays(3), period.endExclusive())
        );
    }

    @Test
    @DisplayName("Rejects missing starts and non-positive durations")
    void rejectsInvalidValues() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new RentalPeriod(null, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new RentalPeriod(START, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new RentalPeriod(START, -1)),
                () -> assertThrows(DateTimeException.class,
                        () -> new RentalPeriod(LocalDateTime.MAX, 1))
        );
    }

    @Test
    @DisplayName("Detects identical, partial, and containing overlaps")
    void detectsEveryOverlapDirection() {
        RentalPeriod original = new RentalPeriod(START, 3);

        assertAll(
                () -> assertTrue(original.overlaps(new RentalPeriod(START, 3))),
                () -> assertTrue(original.overlaps(new RentalPeriod(START.plusHours(1), 1))),
                () -> assertTrue(original.overlaps(new RentalPeriod(START.minusDays(1), 2))),
                () -> assertTrue(original.overlaps(new RentalPeriod(START.plusDays(2), 2))),
                () -> assertTrue(original.overlaps(new RentalPeriod(START.minusDays(1), 10)))
        );
    }

    @Test
    @DisplayName("Allows back-to-back and separated rental periods")
    void allowsAdjacentAndSeparatedPeriods() {
        RentalPeriod original = new RentalPeriod(START, 3);

        assertAll(
                () -> assertFalse(original.overlaps(new RentalPeriod(START.plusDays(3), 1))),
                () -> assertFalse(original.overlaps(new RentalPeriod(START.minusDays(1), 1))),
                () -> assertFalse(original.overlaps(new RentalPeriod(START.plusDays(5), 1)))
        );
    }

    @Test
    @DisplayName("Rejects a null comparison period")
    void rejectsNullComparisonPeriod() {
        RentalPeriod period = new RentalPeriod(START, 1);

        assertThrows(NullPointerException.class, () -> period.overlaps(null));
    }
}

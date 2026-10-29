package com.nivetha.carrental.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable rental interval with a start date/time and a positive duration in days.
 * The end is exclusive, so a rental may start exactly when another rental ends.
 */
public record RentalPeriod(LocalDateTime start, int days) {

    public RentalPeriod {
        Objects.requireNonNull(start, "Start date/time must not be null");
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be greater than zero");
        }
        // Validate that the duration can be represented before accepting the value object.
        start.plusDays(days);
    }

    public LocalDateTime endExclusive() {
        return start.plusDays(days);
    }

    /** Returns true when this interval and the other interval share any time. */
    public boolean overlaps(RentalPeriod other) {
        Objects.requireNonNull(other, "Other rental period must not be null");
        return start.isBefore(other.endExclusive())
                && other.start().isBefore(endExclusive());
    }
}

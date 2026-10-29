package com.nivetha.carrental.service;

import com.nivetha.carrental.domain.CarType;
import com.nivetha.carrental.domain.RentalPeriod;

/** Raised when every car of the requested type is already reserved for the period. */
public final class NoCarAvailableException extends RuntimeException {

    public NoCarAvailableException(CarType type, RentalPeriod period) {
        super("No " + type.displayName() + " is available from "
                + period.start() + " to " + period.endExclusive() + " (end exclusive)");
    }
}

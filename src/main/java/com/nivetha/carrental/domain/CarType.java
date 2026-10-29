package com.nivetha.carrental.domain;

/** The vehicle categories supported by the rental system. */
public enum CarType {
    SEDAN("Sedan"),
    SUV("SUV"),
    VAN("Van");

    private final String displayName;

    CarType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

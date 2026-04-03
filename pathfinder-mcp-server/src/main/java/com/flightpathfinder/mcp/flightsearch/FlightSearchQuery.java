package com.flightpathfinder.mcp.flightsearch;

import java.time.LocalDate;
import java.util.Locale;

public record FlightSearchQuery(
        String origin,
        String destination,
        LocalDate date,
        int flexibilityDays,
        int topK) {

    public FlightSearchQuery {
        origin = normalizeCode(origin);
        destination = normalizeCode(destination);
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

package com.flightpathfinder.mcp.riskevaluate;

import java.util.Locale;

public record RiskEvaluateQuery(
        String hubAirport,
        String firstAirline,
        String secondAirline,
        double bufferHours) {

    public RiskEvaluateQuery {
        hubAirport = normalizeAirport(hubAirport);
        firstAirline = normalizeAirline(firstAirline);
        secondAirline = normalizeAirline(secondAirline);
        if (bufferHours <= 0D) {
            throw new IllegalArgumentException("bufferHours must be greater than 0");
        }
    }

    private static String normalizeAirport(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeAirline(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

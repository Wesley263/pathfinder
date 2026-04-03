package com.flightpathfinder.mcp.flightsearch;

public record FlightSearchOption(
        String airlineCode,
        String airlineName,
        String airlineType,
        String origin,
        String destination,
        String date,
        double priceCny,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        boolean lowCostCarrier) {
}

package com.flightpathfinder.mcp.pricelookup;

public record PriceLookupItem(
        String cityPair,
        String origin,
        String destination,
        String airlineCode,
        String airlineName,
        String airlineType,
        String date,
        double lowestPriceCny,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        boolean lowCostCarrier) {
}

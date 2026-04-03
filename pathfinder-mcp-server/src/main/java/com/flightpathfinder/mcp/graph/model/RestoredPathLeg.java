package com.flightpathfinder.mcp.graph.model;

public record RestoredPathLeg(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops) {
}

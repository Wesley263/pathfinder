package com.flightpathfinder.mcp.graph.model;

public record RestoredGraphEdge(
        String edgeId,
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops) {
}


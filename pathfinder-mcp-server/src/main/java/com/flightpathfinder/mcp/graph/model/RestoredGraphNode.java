package com.flightpathfinder.mcp.graph.model;

public record RestoredGraphNode(
        String airportCode,
        String airportName,
        String cityName,
        String countryCode,
        double latitude,
        double longitude,
        String timezone,
        int minTransferMinutes) {
}


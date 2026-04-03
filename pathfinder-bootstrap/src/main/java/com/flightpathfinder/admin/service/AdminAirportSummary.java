package com.flightpathfinder.admin.service;

import java.time.Instant;

public record AdminAirportSummary(
        String iataCode,
        String icaoCode,
        String nameEn,
        String nameCn,
        String city,
        String cityCn,
        String countryCode,
        double latitude,
        double longitude,
        String timezone,
        String type,
        String source,
        long outgoingRouteCount,
        long incomingRouteCount,
        long totalRouteCount,
        String currentSnapshotStatus,
        boolean presentInCurrentSnapshot,
        String currentSnapshotVersion,
        Instant observedAt) {
}

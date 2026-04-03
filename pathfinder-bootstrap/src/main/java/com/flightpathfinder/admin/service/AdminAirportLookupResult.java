package com.flightpathfinder.admin.service;

import java.time.Instant;

public record AdminAirportLookupResult(
        String iataCode,
        String status,
        String message,
        Instant checkedAt,
        AdminAirportSummary airport) {

    public AdminAirportLookupResult {
        iataCode = iataCode == null ? "" : iataCode.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
    }
}

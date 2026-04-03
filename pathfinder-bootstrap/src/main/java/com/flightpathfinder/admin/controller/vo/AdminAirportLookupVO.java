package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

public record AdminAirportLookupVO(
        String iataCode,
        String status,
        String message,
        Instant checkedAt,
        AdminAirportSummaryVO airport) {
}

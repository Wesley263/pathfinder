package com.flightpathfinder.admin.service;

import java.time.Instant;

/**
 * 管理端服务层数据模型。
 */
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


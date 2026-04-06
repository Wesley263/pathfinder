package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
public record AdminAirportSummaryVO(
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


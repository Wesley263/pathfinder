package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminFlightDiagnosticOption(
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


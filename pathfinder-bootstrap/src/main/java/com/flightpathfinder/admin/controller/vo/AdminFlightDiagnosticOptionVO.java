package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminFlightDiagnosticOptionVO(
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


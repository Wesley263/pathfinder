package com.flightpathfinder.admin.controller.vo;

public record AdminPathDiagnosticLegVO(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm) {
}

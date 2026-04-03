package com.flightpathfinder.admin.service;

public record AdminPathDiagnosticLeg(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm) {
}

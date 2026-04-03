package com.flightpathfinder.admin.service;

public record AdminFlightDiagnosticQuery(
        String origin,
        String destination,
        String date,
        int flexibilityDays,
        int topK) {
}

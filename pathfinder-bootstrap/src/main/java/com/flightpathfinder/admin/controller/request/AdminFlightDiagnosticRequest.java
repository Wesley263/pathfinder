package com.flightpathfinder.admin.controller.request;

public record AdminFlightDiagnosticRequest(
        String origin,
        String destination,
        String date,
        int flexibilityDays,
        int topK) {
}

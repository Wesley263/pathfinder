package com.flightpathfinder.admin.controller.request;

public record AdminPathDiagnosticRequest(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}

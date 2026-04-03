package com.flightpathfinder.admin.service;

public record AdminPathDiagnosticQuery(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}

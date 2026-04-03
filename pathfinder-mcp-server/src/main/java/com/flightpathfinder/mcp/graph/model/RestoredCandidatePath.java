package com.flightpathfinder.mcp.graph.model;

import java.util.List;

public record RestoredCandidatePath(
        List<RestoredPathLeg> legs,
        double totalPriceCny,
        int totalDurationMinutes,
        double totalDistanceKm,
        double averageOnTimeRate) {

    public int segmentCount() {
        return legs.size();
    }

    public int transferCount() {
        return Math.max(0, legs.size() - 1);
    }

    public boolean isDirect() {
        return legs.size() == 1;
    }

    public boolean baggageIncluded() {
        return legs.stream().allMatch(RestoredPathLeg::baggageIncluded);
    }

    public int totalStops() {
        return legs.stream().mapToInt(RestoredPathLeg::stops).sum();
    }

    public double averageCompetitionCount() {
        return legs.stream()
                .mapToInt(RestoredPathLeg::competitionCount)
                .average()
                .orElse(0.0);
    }

    public List<String> hubAirports() {
        if (legs.size() <= 1) {
            return List.of();
        }
        return legs.stream()
                .limit(legs.size() - 1L)
                .map(RestoredPathLeg::destination)
                .toList();
    }
}

package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import java.util.HashSet;
import java.util.Set;

final class GraphPathPartialState {

    private final String currentAirport;
    private final Set<String> visitedAirports;
    private final int segmentsUsed;
    private final double totalPriceCny;
    private final int totalDurationMinutes;
    private final double totalDistanceKm;
    private final double onTimeRateSum;
    private final int competitionCountSum;
    private final int totalStops;
    private final boolean baggageIncluded;

    private GraphPathPartialState(String currentAirport,
                                  Set<String> visitedAirports,
                                  int segmentsUsed,
                                  double totalPriceCny,
                                  int totalDurationMinutes,
                                  double totalDistanceKm,
                                  double onTimeRateSum,
                                  int competitionCountSum,
                                  int totalStops,
                                  boolean baggageIncluded) {
        this.currentAirport = currentAirport;
        this.visitedAirports = Set.copyOf(visitedAirports);
        this.segmentsUsed = segmentsUsed;
        this.totalPriceCny = totalPriceCny;
        this.totalDurationMinutes = totalDurationMinutes;
        this.totalDistanceKm = totalDistanceKm;
        this.onTimeRateSum = onTimeRateSum;
        this.competitionCountSum = competitionCountSum;
        this.totalStops = totalStops;
        this.baggageIncluded = baggageIncluded;
    }

    static GraphPathPartialState root(String originAirport) {
        return new GraphPathPartialState(
                originAirport,
                Set.of(originAirport),
                0,
                0.0,
                0,
                0.0,
                0.0,
                0,
                0,
                true);
    }

    GraphPathPartialState advance(RestoredGraphEdge edge, int transferMinutes, int stopoverMinutes) {
        Set<String> nextVisitedAirports = new HashSet<>(visitedAirports);
        nextVisitedAirports.add(edge.destination());
        int additionalDuration = edge.durationMinutes();
        if (segmentsUsed > 0) {
            additionalDuration += transferMinutes + stopoverMinutes;
        }
        return new GraphPathPartialState(
                edge.destination(),
                nextVisitedAirports,
                segmentsUsed + 1,
                totalPriceCny + edge.basePriceCny(),
                totalDurationMinutes + additionalDuration,
                totalDistanceKm + edge.distanceKm(),
                onTimeRateSum + edge.onTimeRate(),
                competitionCountSum + edge.competitionCount(),
                totalStops + edge.stops(),
                baggageIncluded && edge.baggageIncluded());
    }

    String currentAirport() {
        return currentAirport;
    }

    Set<String> visitedAirports() {
        return visitedAirports;
    }

    int segmentsUsed() {
        return segmentsUsed;
    }

    double totalPriceCny() {
        return totalPriceCny;
    }

    int totalDurationMinutes() {
        return totalDurationMinutes;
    }

    double totalDistanceKm() {
        return totalDistanceKm;
    }

    double averageOnTimeRate() {
        if (segmentsUsed == 0) {
            return 0.0;
        }
        return onTimeRateSum / segmentsUsed;
    }

    double averageCompetitionCount() {
        if (segmentsUsed == 0) {
            return 0.0;
        }
        return (double) competitionCountSum / segmentsUsed;
    }

    int totalStops() {
        return totalStops;
    }

    boolean baggageIncluded() {
        return baggageIncluded;
    }
}

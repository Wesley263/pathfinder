package com.flightpathfinder.framework.readmodel.graph;

import java.util.Map;

public record GraphSnapshotEdge(
        String edgeId,
        String fromNodeId,
        String toNodeId,
        String carrierCode,
        String carrierName,
        String carrierType,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops,
        Map<String, Object> attributes) {
}

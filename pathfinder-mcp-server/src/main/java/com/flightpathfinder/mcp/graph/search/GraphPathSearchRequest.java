package com.flightpathfinder.mcp.graph.search;

public record GraphPathSearchRequest(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}


package com.flightpathfinder.mcp.graph.search.fixture;

import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
/**
 * 测试用例。
 */
public final class TestFlightGraphBuilder {

    private final RestoredFlightGraph.Builder delegate = RestoredFlightGraph.builder();

    public TestFlightGraphBuilder addNode(String airportCode, double latitude, double longitude) {
        delegate.addNode(new RestoredGraphNode(
                airportCode,
                airportCode + " Airport",
                airportCode + " City",
                "XX",
                latitude,
                longitude,
                "UTC",
                45));
        return this;
    }

    public TestFlightGraphBuilder addEdge(String edgeId,
                                          String origin,
                                          String destination,
                                          double basePriceCny,
                                          int durationMinutes,
                                          double distanceKm,
                                          double onTimeRate,
                                          boolean baggageIncluded,
                                          int competitionCount,
                                          int stops) {
        delegate.addEdge(new RestoredGraphEdge(
                edgeId,
                origin,
                destination,
                "MU",
                "Test Carrier",
                "FULL_SERVICE",
                basePriceCny,
                durationMinutes,
                distanceKm,
                onTimeRate,
                baggageIncluded,
                competitionCount,
                stops));
        return this;
    }

    public RestoredFlightGraph build() {
        return delegate.build();
    }
}


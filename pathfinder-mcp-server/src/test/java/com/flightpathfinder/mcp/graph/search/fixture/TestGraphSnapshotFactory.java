package com.flightpathfinder.mcp.graph.search.fixture;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.Arrays;
import java.util.List;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
public final class TestGraphSnapshotFactory {

    private TestGraphSnapshotFactory() {
    }

    public static RestoredFlightGraph branchingGraphAtoC() {
        return new TestFlightGraphBuilder()
                .addNode("A", 0.0, 0.0)
                .addNode("B", 0.0, 1.0)
                .addNode("C", 0.0, 2.0)
                .addNode("D", 1.0, 1.0)
                .addNode("E", 5.0, 5.0)
                .addEdge("ab", "A", "B", 120, 70, 111, 0.95, true, 8, 0)
                .addEdge("bc", "B", "C", 130, 80, 111, 0.94, true, 7, 0)
                .addEdge("ad", "A", "D", 150, 120, 157, 0.88, false, 3, 0)
                .addEdge("dc", "D", "C", 90, 70, 157, 0.82, false, 2, 1)
                .build();
    }

    public static RestoredFlightGraph detourGraphAtoC() {
        return new TestFlightGraphBuilder()
                .addNode("A", 0.0, 0.0)
                .addNode("B", 0.0, 1.0)
                .addNode("C", 0.0, 2.0)
                .addNode("X", 0.0, 20.0)
                .addNode("Y", 0.0, 30.0)
                .addEdge("ab", "A", "B", 100, 70, 111, 0.95, true, 8, 0)
                .addEdge("bc", "B", "C", 110, 75, 111, 0.94, true, 8, 0)
                .addEdge("ax", "A", "X", 400, 300, 2220, 0.70, false, 2, 0)
                .addEdge("xy", "X", "Y", 300, 220, 1110, 0.65, false, 1, 0)
                .addEdge("yc", "Y", "C", 350, 320, 3110, 0.60, false, 1, 1)
                .build();
    }

    public static RestoredFlightGraph disconnectedGraph() {
        return new TestFlightGraphBuilder()
                .addNode("A", 0.0, 0.0)
                .addNode("B", 0.0, 1.0)
                .addNode("C", 0.0, 2.0)
                .addNode("D", 10.0, 10.0)
                .addNode("E", 11.0, 11.0)
                .addEdge("ab", "A", "B", 100, 60, 111, 0.90, true, 5, 0)
                .addEdge("bc", "B", "C", 100, 60, 111, 0.90, true, 5, 0)
                .addEdge("de", "D", "E", 100, 60, 150, 0.90, true, 5, 0)
                .build();
    }

    public static RestoredCandidatePath candidatePathFromEdges(RestoredGraphEdge... edges) {
        List<RestoredGraphEdge> edgeList = Arrays.asList(edges);
        List<RestoredPathLeg> legs = edgeList.stream()
                .map(edge -> new RestoredPathLeg(
                        edge.origin(),
                        edge.destination(),
                        edge.carrierCode(),
                        edge.carrierName(),
                        edge.carrierType(),
                        edge.basePriceCny(),
                        edge.durationMinutes(),
                        edge.distanceKm(),
                        edge.onTimeRate(),
                        edge.baggageIncluded(),
                        edge.competitionCount(),
                        edge.stops()))
                .toList();

        double totalPrice = edgeList.stream().mapToDouble(RestoredGraphEdge::basePriceCny).sum();
        int totalDuration = edgeList.stream().mapToInt(RestoredGraphEdge::durationMinutes).sum();
        double totalDistance = edgeList.stream().mapToDouble(RestoredGraphEdge::distanceKm).sum();
        double averageOnTime = edgeList.stream().mapToDouble(RestoredGraphEdge::onTimeRate).average().orElse(0.0);

        return new RestoredCandidatePath(legs, totalPrice, totalDuration, totalDistance, averageOnTime);
    }
}




package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathPartialStateTest {

    @Test
    void root_shouldInitializeWithOriginAndEmptyMetrics() {
        GraphPathPartialState root = GraphPathPartialState.root("SHA");

        assertEquals("SHA", root.currentAirport());
        assertTrue(root.visitedAirports().contains("SHA"));
        assertEquals(0, root.segmentsUsed());
        assertEquals(0.0, root.totalPriceCny());
        assertEquals(0, root.totalDurationMinutes());
        assertEquals(0.0, root.totalDistanceKm());
        assertEquals(0.0, root.averageOnTimeRate());
        assertEquals(0.0, root.averageCompetitionCount());
        assertEquals(0, root.totalStops());
        assertTrue(root.baggageIncluded());
    }

    @Test
    void advance_shouldAccumulateMetricsAndApplyTransferAfterFirstSegment() {
        GraphPathPartialState root = GraphPathPartialState.root("SHA");
        RestoredGraphEdge leg1 = edge("e1", "SHA", "BKK", 1000, 180, 1800, 0.90, true, 8, 0);
        RestoredGraphEdge leg2 = edge("e2", "BKK", "LON", 2500, 720, 9500, 0.80, false, 4, 1);

        GraphPathPartialState state1 = root.advance(leg1, 45, 120);
        GraphPathPartialState state2 = state1.advance(leg2, 45, 120);

        assertEquals("BKK", state1.currentAirport());
        assertEquals(1, state1.segmentsUsed());
        assertEquals(1000, state1.totalPriceCny());
        assertEquals(180, state1.totalDurationMinutes());

        assertEquals("LON", state2.currentAirport());
        assertTrue(state2.visitedAirports().contains("LON"));
        assertEquals(2, state2.segmentsUsed());
        assertEquals(3500, state2.totalPriceCny());
        assertEquals(1065, state2.totalDurationMinutes());
        assertEquals(11300, state2.totalDistanceKm());
        assertEquals(0.85, state2.averageOnTimeRate(), 1.0e-9);
        assertEquals(6.0, state2.averageCompetitionCount());
        assertEquals(1, state2.totalStops());
        assertFalse(state2.baggageIncluded());
    }

    private static RestoredGraphEdge edge(String edgeId,
                                          String origin,
                                          String destination,
                                          double price,
                                          int duration,
                                          double distance,
                                          double onTimeRate,
                                          boolean baggage,
                                          int competition,
                                          int stops) {
        return new RestoredGraphEdge(
                edgeId,
                origin,
                destination,
                "MU",
                "China Eastern",
                "FULL_SERVICE",
                price,
                duration,
                distance,
                onTimeRate,
                baggage,
                competition,
                stops);
    }
}




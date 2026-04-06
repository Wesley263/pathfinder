package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathPartialFrontierTest {

    @Test
    void isDominated_shouldDetectDominatedStateAtSameAirport() {
        GraphPathPartialFrontier frontier = new GraphPathPartialFrontier();
        GraphPathPartialState root = GraphPathPartialState.root("A");

        GraphPathPartialState fastToB = root.advance(edge("ab", "A", "B", 100, 60, 110, 0.95, true, 8, 0), 0, 0);
        GraphPathPartialState slowToBViaC = root
                .advance(edge("ac", "A", "C", 120, 80, 130, 0.85, true, 5, 0), 0, 0)
                .advance(edge("cb", "C", "B", 200, 120, 220, 0.80, true, 3, 1), 45, 0);

        frontier.record(fastToB);

        assertTrue(frontier.isDominated(slowToBViaC));
        assertFalse(frontier.isDominated(fastToB));
    }

    @Test
    void recordAndDiscard_shouldReplaceDominatedAndCleanBucket() {
        GraphPathPartialFrontier frontier = new GraphPathPartialFrontier();
        GraphPathPartialState root = GraphPathPartialState.root("A");

        GraphPathPartialState fastToB = root.advance(edge("ab", "A", "B", 100, 60, 110, 0.95, true, 8, 0), 0, 0);
        GraphPathPartialState slowToBViaC = root
                .advance(edge("ac", "A", "C", 120, 80, 130, 0.85, true, 5, 0), 0, 0)
                .advance(edge("cb", "C", "B", 200, 120, 220, 0.80, true, 3, 1), 45, 0);

        frontier.record(slowToBViaC);
        assertFalse(frontier.isDominated(fastToB));

        frontier.record(fastToB);
        assertTrue(frontier.isDominated(slowToBViaC));

        frontier.discard(fastToB);
        assertFalse(frontier.isDominated(slowToBViaC));
    }

    private static RestoredGraphEdge edge(String edgeId,
                                          String origin,
                                          String destination,
                                          double basePriceCny,
                                          int durationMinutes,
                                          double distanceKm,
                                          double onTimeRate,
                                          boolean baggageIncluded,
                                          int competitionCount,
                                          int stops) {
        return new RestoredGraphEdge(
                edgeId,
                origin,
                destination,
                "MU",
                "Carrier",
                "FULL_SERVICE",
                basePriceCny,
                durationMinutes,
                distanceKm,
                onTimeRate,
                baggageIncluded,
                competitionCount,
                stops);
    }
}




package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.search.fixture.TestGraphSnapshotFactory;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class GraphPathSearchLowerBoundsTest {

    @Test
    void prepare_shouldComputeLowerBoundsForBranchingGraph() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();

        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(graph, "A", "C");

        assertEquals(0, lowerBounds.minRemainingSegments("C"));
        assertEquals(1, lowerBounds.minRemainingSegments("B"));
        assertEquals(1, lowerBounds.minRemainingSegments("D"));
        assertEquals(2, lowerBounds.minRemainingSegments("A"));

        assertEquals(0.0, lowerBounds.minRemainingPriceCny("C"), 1.0e-9);
        assertEquals(130.0, lowerBounds.minRemainingPriceCny("B"), 1.0e-9);
        assertEquals(90.0, lowerBounds.minRemainingPriceCny("D"), 1.0e-9);
        assertEquals(240.0, lowerBounds.minRemainingPriceCny("A"), 1.0e-9);

        assertEquals(0, lowerBounds.minRemainingDurationMinutes("C"));
        assertEquals(80, lowerBounds.minRemainingDurationMinutes("B"));
        assertEquals(70, lowerBounds.minRemainingDurationMinutes("D"));
        assertEquals(150, lowerBounds.minRemainingDurationMinutes("A"));
    }

    @Test
    void canReachChecks_shouldRespectSegmentsAndBudgetAndUnreachableNodes() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();
        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(graph, "A", "C");

        assertFalse(lowerBounds.canReachWithinSegments("A", 1));
        assertTrue(lowerBounds.canReachWithinSegments("A", 2));
        assertFalse(lowerBounds.canReachWithinBudget("A", 0.0, 239.99));
        assertTrue(lowerBounds.canReachWithinBudget("A", 0.0, 240.0));

        assertEquals(Integer.MAX_VALUE, lowerBounds.minRemainingSegments("E"));
        assertEquals(Double.POSITIVE_INFINITY, lowerBounds.minRemainingPriceCny("E"));
        assertEquals(Integer.MAX_VALUE, lowerBounds.minRemainingDurationMinutes("E"));
        assertFalse(lowerBounds.canReachWithinSegments("E", 99));
        assertFalse(lowerBounds.canReachWithinBudget("E", 0.0, 9999.0));
    }

    @Test
    void estimatedDetourRatio_shouldBeOneAtOriginAndGrowWithTravelDistance() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();
        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(graph, "A", "C");

        assertEquals(1.0, lowerBounds.estimatedDetourRatio("A", 0.0), 1.0e-9);
        assertTrue(lowerBounds.estimatedDetourRatio("B", 200.0) > 1.0);
    }
}


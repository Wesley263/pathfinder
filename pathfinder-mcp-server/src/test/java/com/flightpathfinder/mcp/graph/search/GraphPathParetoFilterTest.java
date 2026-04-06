package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathParetoFilterTest {

    @Test
    void selectForRanking_shouldReturnEmptySelectionWhenNoCandidates() {
        GraphPathParetoFilter filter = new GraphPathParetoFilter();

        GraphPathParetoSelection selection = filter.selectForRanking(List.of(), 5);

        assertTrue(selection.selectedCandidates().isEmpty());
        assertEquals(0, selection.maxLayer());
    }

    @Test
    void selectForRanking_shouldAssignParetoLayersWhenMinimumRequiresAll() {
        GraphPathParetoFilter filter = new GraphPathParetoFilter();
        RestoredCandidatePath dominant = candidate("dom", 100, 100, 0.95, true, 9, 0);
        RestoredCandidatePath tradeoff = candidate("trade", 80, 160, 0.90, true, 7, 0);
        RestoredCandidatePath dominated = candidate("bad", 120, 130, 0.80, false, 4, 1);

        GraphPathParetoSelection selection = filter.selectForRanking(
                List.of(dominated, dominant, tradeoff),
                10);

        assertEquals(3, selection.selectedCandidates().size());
        assertEquals(List.of(tradeoff, dominant, dominated), selection.selectedCandidates());
        assertEquals(1, selection.layerOf(tradeoff));
        assertEquals(1, selection.layerOf(dominant));
        assertEquals(2, selection.layerOf(dominated));
        assertEquals(2, selection.maxLayer());
    }

    @Test
    void selectForRanking_shouldStopAfterEnoughFrontierCandidates() {
        GraphPathParetoFilter filter = new GraphPathParetoFilter();
        RestoredCandidatePath dominant = candidate("dom", 100, 100, 0.95, true, 9, 0);
        RestoredCandidatePath tradeoff = candidate("trade", 80, 160, 0.90, true, 7, 0);
        RestoredCandidatePath dominated = candidate("bad", 120, 130, 0.80, false, 4, 1);

        GraphPathParetoSelection selection = filter.selectForRanking(
                List.of(dominated, dominant, tradeoff),
                2);

        assertEquals(2, selection.selectedCandidates().size());
        assertTrue(selection.selectedCandidates().contains(dominant));
        assertTrue(selection.selectedCandidates().contains(tradeoff));
        assertFalse(selection.selectedCandidates().contains(dominated));
    }

    private static RestoredCandidatePath candidate(String id,
                                                   double totalPriceCny,
                                                   int totalDurationMinutes,
                                                   double onTimeRate,
                                                   boolean baggageIncluded,
                                                   int competitionCount,
                                                   int stops) {
        RestoredPathLeg leg = new RestoredPathLeg(
                "A",
                "C",
                "MU",
                "Carrier-" + id,
                "FULL_SERVICE",
                totalPriceCny,
                totalDurationMinutes,
                1000.0,
                onTimeRate,
                baggageIncluded,
                competitionCount,
                stops);
        return new RestoredCandidatePath(
                List.of(leg),
                totalPriceCny,
                totalDurationMinutes,
                1000.0,
                onTimeRate);
    }
}




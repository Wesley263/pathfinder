package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class GraphPathWeightedRankerTest {

    @Test
    void rank_shouldReturnEmptyWhenNoCandidates() {
        GraphPathWeightedRanker ranker = new GraphPathWeightedRanker();
        GraphPathParetoSelection selection = new GraphPathParetoSelection(List.of(), Map.of(), 1);

        List<RestoredCandidatePath> ranked = ranker.rank(
                RestoredFlightGraph.builder().build(),
                selection,
                GraphPathScoringProfile.defaultProfile(),
                3);

        assertTrue(ranked.isEmpty());
    }

    @Test
    void rank_shouldSortByCompositeScoreAndApplyTopK() {
        RestoredCandidatePath better = candidate("SHA", "TYO", 1200, 240, 1800, 0.95, true, 9, 0);
        RestoredCandidatePath worse = candidate("SHA", "TYO", 3200, 460, 2200, 0.70, false, 2, 1);

        GraphPathParetoSelection selection = new GraphPathParetoSelection(
                List.of(worse, better),
                Map.of(better, 1, worse, 2),
                2);

        GraphPathWeightedRanker ranker = new GraphPathWeightedRanker();
        List<RestoredCandidatePath> ranked = ranker.rank(
                RestoredFlightGraph.builder().build(),
                selection,
                GraphPathScoringProfile.defaultProfile(),
                1);

        assertEquals(1, ranked.size());
        assertEquals(better, ranked.get(0));
    }

    private static RestoredCandidatePath candidate(String origin,
                                                   String destination,
                                                   double price,
                                                   int duration,
                                                   double distance,
                                                   double onTimeRate,
                                                   boolean baggage,
                                                   int competition,
                                                   int stops) {
        RestoredPathLeg leg = new RestoredPathLeg(
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
        return new RestoredCandidatePath(List.of(leg), price, duration, distance, onTimeRate);
    }
}


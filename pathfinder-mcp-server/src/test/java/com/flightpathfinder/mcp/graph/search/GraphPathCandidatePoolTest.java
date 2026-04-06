package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class GraphPathCandidatePoolTest {

    @Test
    void admit_shouldKeepBestCandidatesWithinCapacity() {
        GraphPathCandidatePool pool = new GraphPathCandidatePool(2);
        RestoredCandidatePath c1 = candidate("SHA", "BKK", 1000, 300, 1800, 0.9, true, 6, 0);
        RestoredCandidatePath c2 = candidate("SHA", "NRT", 1200, 320, 1900, 0.92, true, 7, 0);
        RestoredCandidatePath c3 = candidate("SHA", "ICN", 1100, 310, 1700, 0.88, false, 4, 1);

        pool.admit(c1, 0.50);
        pool.admit(c2, 0.70);
        pool.admit(c3, 0.60);

        List<RestoredCandidatePath> admitted = pool.admittedCandidates();
        assertEquals(2, admitted.size());
        assertEquals(c2, admitted.get(0));
        assertEquals(c3, admitted.get(1));
    }

    @Test
    void shouldRejectPotential_shouldRespectWorstScoreWithSlack() {
        GraphPathCandidatePool pool = new GraphPathCandidatePool(2);
        pool.admit(candidate("A", "B", 1000, 200, 1000, 0.9, true, 5, 0), 0.80);

        assertFalse(pool.shouldRejectPotential(0.10, 0.01));

        pool.admit(candidate("A", "C", 1100, 210, 1100, 0.9, true, 5, 0), 0.70);

        assertTrue(pool.shouldRejectPotential(0.60, 0.05));
        assertFalse(pool.shouldRejectPotential(0.68, 0.05));
    }

    @Test
    void shouldStop_shouldReturnTrueWhenFrontierCannotBeatPoolWorstScore() {
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "SHA", "LON", 6000, 0, 1, 1);
        GraphPathFrontierPolicy policy = GraphPathFrontierPolicy.defaultPolicy(request);
        GraphPathCandidatePool pool = new GraphPathCandidatePool(policy.candidatePoolSize());

        for (int index = 1; index <= policy.earlyStopCandidateCount(); index++) {
            pool.admit(candidate("S" + index, "D" + index, 1000 + index, 200, 1000, 0.8, true, 3, 0), index);
        }

        assertTrue(pool.shouldStop(0.50, policy));
        assertFalse(pool.shouldStop(80.0, policy));
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


package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathSearchProfileTest {

    @Test
    void defaultFor_shouldBuildPolicyAndScoringProfileFromRequest() {
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "SHA", "LON", 6000, 1, 2, 5);

        GraphPathSearchProfile profile = GraphPathSearchProfile.defaultFor(request);

        assertEquals(1600, profile.frontierPolicy().maxFrontierSize());
        assertEquals(12800, profile.frontierPolicy().maxExpansions());
        assertEquals(120, profile.frontierPolicy().candidatePoolSize());
        assertEquals(25, profile.frontierPolicy().minimumParetoSelectionCount());
        assertEquals(0.28, profile.scoringProfile().priceWeight());
        assertEquals(0.23, profile.scoringProfile().durationWeight());
    }
}




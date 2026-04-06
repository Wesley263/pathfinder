package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathScoringProfileTest {

    @Test
    void defaultProfile_shouldExposeStableWeights() {
        GraphPathScoringProfile profile = GraphPathScoringProfile.defaultProfile();

        assertEquals(0.28, profile.priceWeight());
        assertEquals(0.23, profile.durationWeight());
        assertEquals(0.18, profile.reliabilityWeight());
        assertEquals(0.11, profile.transferWeight());
        assertEquals(0.05, profile.stopWeight());
        assertEquals(0.05, profile.baggageWeight());
        assertEquals(0.06, profile.detourWeight());
        assertEquals(0.02, profile.competitionWeight());
        assertEquals(0.02, profile.paretoLayerWeight());
        assertEquals(2.20, profile.detourReferenceRatio());
        assertEquals(8.0, profile.competitionReference());
    }
}




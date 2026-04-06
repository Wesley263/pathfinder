package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathFrontierPolicyTest {

    @Test
    void defaultPolicy_shouldDeriveExpectedBoundsFromRequest() {
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "SHA", "LON", 5000, 0, 3, 4);

        GraphPathFrontierPolicy policy = GraphPathFrontierPolicy.defaultPolicy(request);

        assertEquals(1920, policy.maxFrontierSize());
        assertEquals(15360, policy.maxExpansions());
        assertEquals(96, policy.candidatePoolSize());
        assertEquals(60, policy.earlyStopCandidateCount());
        assertEquals(20, policy.minimumParetoSelectionCount());
        assertEquals(0.05, policy.candidateAdmissionSlack());
        assertEquals(0.03, policy.frontierClosureSlack());
        assertEquals(1.95, policy.detourBaseThreshold());
        assertEquals(0.35, policy.detourPerExtraSegmentThreshold());
    }
}




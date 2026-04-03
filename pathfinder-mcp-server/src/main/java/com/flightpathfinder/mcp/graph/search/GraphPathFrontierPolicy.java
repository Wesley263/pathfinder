package com.flightpathfinder.mcp.graph.search;

/**
 * Frontier and admission policy for bounded best-first search.
 *
 * <p>These controls keep the search from drifting into unbounded enumeration while still
 * leaving enough room for good candidates to survive pruning and ranking.</p>
 */
final class GraphPathFrontierPolicy {

    private final int maxFrontierSize;
    private final int maxExpansions;
    private final int candidatePoolSize;
    private final int earlyStopCandidateCount;
    private final int minimumParetoSelectionCount;
    private final double candidateAdmissionSlack;
    private final double frontierClosureSlack;
    private final double detourBaseThreshold;
    private final double detourPerExtraSegmentThreshold;

    private GraphPathFrontierPolicy(int maxFrontierSize,
                                    int maxExpansions,
                                    int candidatePoolSize,
                                    int earlyStopCandidateCount,
                                    int minimumParetoSelectionCount,
                                    double candidateAdmissionSlack,
                                    double frontierClosureSlack,
                                    double detourBaseThreshold,
                                    double detourPerExtraSegmentThreshold) {
        this.maxFrontierSize = maxFrontierSize;
        this.maxExpansions = maxExpansions;
        this.candidatePoolSize = candidatePoolSize;
        this.earlyStopCandidateCount = earlyStopCandidateCount;
        this.minimumParetoSelectionCount = minimumParetoSelectionCount;
        this.candidateAdmissionSlack = candidateAdmissionSlack;
        this.frontierClosureSlack = frontierClosureSlack;
        this.detourBaseThreshold = detourBaseThreshold;
        this.detourPerExtraSegmentThreshold = detourPerExtraSegmentThreshold;
    }

    /**
     * Derives the default frontier policy for one search request.
     */
    static GraphPathFrontierPolicy defaultPolicy(GraphPathSearchRequest request) {
        int maxFrontierSize = Math.max(320, request.topK() * request.maxSegments() * 160);
        int candidatePoolSize = Math.max(80, request.topK() * 24);
        int maxExpansions = Math.max(1600, maxFrontierSize * 8);
        int earlyStopCandidateCount = Math.max(request.topK() * 6, Math.min((candidatePoolSize * 3) / 4, 60));
        int minimumParetoSelectionCount = Math.max(request.topK() * 5, request.topK());
        // Slack values intentionally leave a narrow buffer so the search does not stop too
        // aggressively when near-tied candidates still exist deeper in the frontier.
        return new GraphPathFrontierPolicy(
                maxFrontierSize,
                maxExpansions,
                candidatePoolSize,
                earlyStopCandidateCount,
                minimumParetoSelectionCount,
                0.05,
                0.03,
                1.95,
                0.35);
    }

    int maxFrontierSize() {
        return maxFrontierSize;
    }

    int maxExpansions() {
        return maxExpansions;
    }

    int candidatePoolSize() {
        return candidatePoolSize;
    }

    int earlyStopCandidateCount() {
        return earlyStopCandidateCount;
    }

    int minimumParetoSelectionCount() {
        return minimumParetoSelectionCount;
    }

    double candidateAdmissionSlack() {
        return candidateAdmissionSlack;
    }

    double frontierClosureSlack() {
        return frontierClosureSlack;
    }

    double detourBaseThreshold() {
        return detourBaseThreshold;
    }

    double detourPerExtraSegmentThreshold() {
        return detourPerExtraSegmentThreshold;
    }
}

package com.flightpathfinder.mcp.graph.search;

final class GraphPathSearchProfile {

    private final GraphPathFrontierPolicy frontierPolicy;
    private final GraphPathScoringProfile scoringProfile;

    private GraphPathSearchProfile(GraphPathFrontierPolicy frontierPolicy,
                                   GraphPathScoringProfile scoringProfile) {
        this.frontierPolicy = frontierPolicy;
        this.scoringProfile = scoringProfile;
    }

    static GraphPathSearchProfile defaultFor(GraphPathSearchRequest request) {
        return new GraphPathSearchProfile(
                GraphPathFrontierPolicy.defaultPolicy(request),
                GraphPathScoringProfile.defaultProfile());
    }

    GraphPathFrontierPolicy frontierPolicy() {
        return frontierPolicy;
    }

    GraphPathScoringProfile scoringProfile() {
        return scoringProfile;
    }
}

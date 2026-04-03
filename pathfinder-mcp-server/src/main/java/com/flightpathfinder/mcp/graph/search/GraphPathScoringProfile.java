package com.flightpathfinder.mcp.graph.search;

final class GraphPathScoringProfile {

    private final double priceWeight;
    private final double durationWeight;
    private final double reliabilityWeight;
    private final double transferWeight;
    private final double stopWeight;
    private final double baggageWeight;
    private final double detourWeight;
    private final double competitionWeight;
    private final double paretoLayerWeight;
    private final double detourReferenceRatio;
    private final double competitionReference;

    private GraphPathScoringProfile(double priceWeight,
                                    double durationWeight,
                                    double reliabilityWeight,
                                    double transferWeight,
                                    double stopWeight,
                                    double baggageWeight,
                                    double detourWeight,
                                    double competitionWeight,
                                    double paretoLayerWeight,
                                    double detourReferenceRatio,
                                    double competitionReference) {
        this.priceWeight = priceWeight;
        this.durationWeight = durationWeight;
        this.reliabilityWeight = reliabilityWeight;
        this.transferWeight = transferWeight;
        this.stopWeight = stopWeight;
        this.baggageWeight = baggageWeight;
        this.detourWeight = detourWeight;
        this.competitionWeight = competitionWeight;
        this.paretoLayerWeight = paretoLayerWeight;
        this.detourReferenceRatio = detourReferenceRatio;
        this.competitionReference = competitionReference;
    }

    static GraphPathScoringProfile defaultProfile() {
        return new GraphPathScoringProfile(
                0.28,
                0.23,
                0.18,
                0.11,
                0.05,
                0.05,
                0.06,
                0.02,
                0.02,
                2.20,
                8.0);
    }

    double priceWeight() {
        return priceWeight;
    }

    double durationWeight() {
        return durationWeight;
    }

    double reliabilityWeight() {
        return reliabilityWeight;
    }

    double transferWeight() {
        return transferWeight;
    }

    double stopWeight() {
        return stopWeight;
    }

    double baggageWeight() {
        return baggageWeight;
    }

    double detourWeight() {
        return detourWeight;
    }

    double competitionWeight() {
        return competitionWeight;
    }

    double paretoLayerWeight() {
        return paretoLayerWeight;
    }

    double detourReferenceRatio() {
        return detourReferenceRatio;
    }

    double competitionReference() {
        return competitionReference;
    }
}

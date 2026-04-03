package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;

/**
 * Heuristic scoring and detour-pruning helper for bounded best-first search.
 *
 * <p>This helper keeps search-order math separate from the search loop so frontier expansion,
 * candidate admission, and detour pruning all share the same scoring assumptions.</p>
 */
final class GraphPathSearchHeuristic {

    private final RestoredFlightGraph graph;
    private final GraphPathSearchRequest request;
    private final GraphPathSearchLowerBounds lowerBounds;
    private final GraphPathFrontierPolicy frontierPolicy;
    private final GraphPathScoringProfile scoringProfile;
    private final double priceReferenceCny;
    private final double durationReferenceMinutes;
    private final double transferReference;
    private final double stopReference;

    GraphPathSearchHeuristic(RestoredFlightGraph graph,
                             GraphPathSearchRequest request,
                             GraphPathSearchLowerBounds lowerBounds,
                             GraphPathSearchProfile profile) {
        this.graph = graph;
        this.request = request;
        this.lowerBounds = lowerBounds;
        this.frontierPolicy = profile.frontierPolicy();
        this.scoringProfile = profile.scoringProfile();
        double originMinPrice = finiteDouble(lowerBounds.minRemainingPriceCny(request.origin()), 1200.0);
        int originMinDuration = finiteInt(lowerBounds.minRemainingDurationMinutes(request.origin()), 360);
        this.priceReferenceCny = Math.max(originMinPrice, Math.min(request.maxBudget(), originMinPrice * 3.0));
        int transferPadding = Math.max(0, request.maxSegments() - 1) * (120 + request.stopoverDays() * 24 * 60);
        this.durationReferenceMinutes = Math.max(originMinDuration + transferPadding, originMinDuration * 1.8);
        this.transferReference = Math.max(1, request.maxSegments() - 1);
        this.stopReference = Math.max(1, request.maxSegments() * 2);
    }

    /**
     * Scores a partial state using optimistic lower-bound assumptions.
     */
    double optimisticScore(GraphPathPartialState state) {
        int remainingSegments = lowerBounds.minRemainingSegments(state.currentAirport());
        double estimatedPrice = state.totalPriceCny() + finiteDouble(lowerBounds.minRemainingPriceCny(state.currentAirport()), priceReferenceCny);
        int estimatedDuration = state.totalDurationMinutes() + finiteInt(lowerBounds.minRemainingDurationMinutes(state.currentAirport()), (int) durationReferenceMinutes);
        int estimatedTransfers = Math.max(0, state.segmentsUsed() + remainingSegments - 1);
        double optimisticReliability = optimisticReliability(state, remainingSegments);
        double optimisticCompetition = normalizeUpper(state.averageCompetitionCount() == 0.0 ? scoringProfile.competitionReference() * 0.5 : state.averageCompetitionCount(),
                scoringProfile.competitionReference());
        return composeScore(
                normalizeLower(estimatedPrice, priceReferenceCny),
                normalizeLower(estimatedDuration, durationReferenceMinutes),
                optimisticReliability,
                normalizeLower(estimatedTransfers, transferReference),
                normalizeLower(state.totalStops(), stopReference),
                state.baggageIncluded() ? 1.0 : 0.0,
                detourUtility(lowerBounds.estimatedDetourRatio(state.currentAirport(), state.totalDistanceKm())),
                optimisticCompetition);
    }

    /**
     * Scores a completed candidate path for admission into the bounded pool.
     */
    double candidateScore(RestoredCandidatePath candidate) {
        return composeScore(
                normalizeLower(candidate.totalPriceCny(), priceReferenceCny),
                normalizeLower(candidate.totalDurationMinutes(), durationReferenceMinutes),
                candidate.averageOnTimeRate(),
                normalizeLower(candidate.transferCount(), transferReference),
                normalizeLower(candidate.totalStops(), stopReference),
                candidate.baggageIncluded() ? 1.0 : 0.0,
                detourUtility(actualDetourRatio(candidate)),
                normalizeUpper(candidate.averageCompetitionCount(), scoringProfile.competitionReference()));
    }

    /**
     * Rejects partial states whose estimated detour is already too large to stay competitive.
     */
    boolean shouldPruneByDetour(GraphPathPartialState state) {
        if (state.segmentsUsed() < 2) {
            return false;
        }
        // Detour pruning only starts once the path has enough shape to be meaningful; otherwise
        // early segments would be unfairly penalized before the route has a chance to recover.
        double threshold = frontierPolicy.detourBaseThreshold()
                + Math.max(0, request.maxSegments() - 2) * frontierPolicy.detourPerExtraSegmentThreshold();
        return lowerBounds.estimatedDetourRatio(state.currentAirport(), state.totalDistanceKm()) > threshold;
    }

    private double composeScore(double priceUtility,
                                double durationUtility,
                                double reliabilityUtility,
                                double transferUtility,
                                double stopUtility,
                                double baggageUtility,
                                double detourUtility,
                                double competitionUtility) {
        return priceUtility * scoringProfile.priceWeight()
                + durationUtility * scoringProfile.durationWeight()
                + reliabilityUtility * scoringProfile.reliabilityWeight()
                + transferUtility * scoringProfile.transferWeight()
                + stopUtility * scoringProfile.stopWeight()
                + baggageUtility * scoringProfile.baggageWeight()
                + detourUtility * scoringProfile.detourWeight()
                + competitionUtility * scoringProfile.competitionWeight();
    }

    private double optimisticReliability(GraphPathPartialState state, int remainingSegments) {
        if (state.segmentsUsed() == 0 && remainingSegments == 0) {
            return 1.0;
        }
        double completed = state.averageOnTimeRate() * state.segmentsUsed();
        double bestPossible = completed + Math.max(0, remainingSegments);
        double totalSegments = Math.max(1, state.segmentsUsed() + Math.max(0, remainingSegments));
        return Math.max(0.0, Math.min(1.0, bestPossible / totalSegments));
    }

    private double actualDetourRatio(RestoredCandidatePath candidate) {
        if (candidate.legs().isEmpty()) {
            return 1.0;
        }
        RestoredGraphNode origin = graph.getNode(candidate.legs().get(0).origin());
        RestoredGraphNode destination = graph.getNode(candidate.legs().get(candidate.legs().size() - 1).destination());
        if (origin == null || destination == null || candidate.totalDistanceKm() <= 0.0) {
            return 1.0;
        }
        double directDistance = haversineKm(
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude());
        if (directDistance <= 0.0) {
            return 1.0;
        }
        return candidate.totalDistanceKm() / directDistance;
    }

    private double detourUtility(double detourRatio) {
        if (detourRatio <= 1.0) {
            return 1.0;
        }
        double overage = detourRatio - 1.0;
        return Math.max(0.0, 1.0 - (overage / Math.max(0.1, scoringProfile.detourReferenceRatio() - 1.0)));
    }

    private double normalizeLower(double value, double reference) {
        if (reference <= 0.0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - Math.min(1.0, value / reference));
    }

    private double normalizeUpper(double value, double reference) {
        if (reference <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value / reference));
    }

    private double finiteDouble(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private int finiteInt(int value, int fallback) {
        return value < Integer.MAX_VALUE ? value : fallback;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}

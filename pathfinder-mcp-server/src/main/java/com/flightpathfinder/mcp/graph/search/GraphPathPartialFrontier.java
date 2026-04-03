package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dominance frontier for partial search states.
 *
 * <p>This structure tracks the best known partial states per airport so the search can drop
 * strictly worse states early instead of letting them crowd the expansion frontier.</p>
 */
final class GraphPathPartialFrontier {

    private final Map<String, List<GraphPathPartialState>> statesByAirport = new HashMap<>();

    /**
     * Checks whether a candidate partial state is already dominated by an existing one.
     */
    boolean isDominated(GraphPathPartialState candidate) {
        for (GraphPathPartialState existing : statesByAirport.getOrDefault(candidate.currentAirport(), List.of())) {
            if (dominates(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records a candidate partial state and removes states that it now dominates.
     */
    void record(GraphPathPartialState candidate) {
        List<GraphPathPartialState> bucket = statesByAirport.computeIfAbsent(
                candidate.currentAirport(),
                ignored -> new ArrayList<>());
        bucket.removeIf(existing -> dominates(candidate, existing));
        bucket.add(candidate);
    }

    /**
     * Discards a partial state that has been trimmed from the expansion frontier.
     */
    void discard(GraphPathPartialState candidate) {
        List<GraphPathPartialState> bucket = statesByAirport.get(candidate.currentAirport());
        if (bucket == null) {
            return;
        }
        bucket.remove(candidate);
        if (bucket.isEmpty()) {
            statesByAirport.remove(candidate.currentAirport());
        }
    }

    private boolean dominates(GraphPathPartialState left, GraphPathPartialState right) {
        if (!left.currentAirport().equals(right.currentAirport())) {
            return false;
        }
        // The visited-set containment check protects correctness: a state that already
        // visited more airports cannot always substitute for one with more future options.
        if (!right.visitedAirports().containsAll(left.visitedAirports())) {
            return false;
        }
        boolean noWorseSegments = left.segmentsUsed() <= right.segmentsUsed();
        boolean noWorsePrice = left.totalPriceCny() <= right.totalPriceCny();
        boolean noWorseDuration = left.totalDurationMinutes() <= right.totalDurationMinutes();
        boolean noWorseDistance = left.totalDistanceKm() <= right.totalDistanceKm();
        boolean noWorseStops = left.totalStops() <= right.totalStops();
        boolean noWorseReliability = left.averageOnTimeRate() >= right.averageOnTimeRate();
        boolean noWorseCompetition = left.averageCompetitionCount() >= right.averageCompetitionCount();
        boolean noWorseBaggage = left.baggageIncluded() || !right.baggageIncluded();
        if (!(noWorseSegments
                && noWorsePrice
                && noWorseDuration
                && noWorseDistance
                && noWorseStops
                && noWorseReliability
                && noWorseCompetition
                && noWorseBaggage)) {
            return false;
        }
        return left.segmentsUsed() < right.segmentsUsed()
                || left.totalPriceCny() < right.totalPriceCny()
                || left.totalDurationMinutes() < right.totalDurationMinutes()
                || left.totalDistanceKm() < right.totalDistanceKm()
                || left.totalStops() < right.totalStops()
                || left.averageOnTimeRate() > right.averageOnTimeRate()
                || left.averageCompetitionCount() > right.averageCompetitionCount()
                || (left.baggageIncluded() && !right.baggageIncluded())
                || left.visitedAirports().size() < right.visitedAirports().size();
    }
}

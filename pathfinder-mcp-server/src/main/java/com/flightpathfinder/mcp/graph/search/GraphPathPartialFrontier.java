package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部分搜索状态的支配前沿。
 *
 * 说明。
 * 说明。
 */
final class GraphPathPartialFrontier {

    /** 机场到部分状态集合的索引。 */
    private final Map<String, List<GraphPathPartialState>> statesByAirport = new HashMap<>();

    /**
     * 判断候选部分状态是否已被已有状态支配。
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
     * 记录候选状态，并移除被其反向支配的旧状态。
     */
    void record(GraphPathPartialState candidate) {
        List<GraphPathPartialState> bucket = statesByAirport.computeIfAbsent(
                candidate.currentAirport(),
                ignored -> new ArrayList<>());
        bucket.removeIf(existing -> dominates(candidate, existing));
        bucket.add(candidate);
    }

    /**
     * 说明。
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

    /**
     * 说明。
     */
    private boolean dominates(GraphPathPartialState left, GraphPathPartialState right) {
        if (!left.currentAirport().equals(right.currentAirport())) {
            return false;
        }
        // 访问集合包含关系是正确性关键：访问更多机场的状态并不总能替代未来选择更多的状态。
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

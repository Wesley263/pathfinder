package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pareto-layer selector for admitted candidate paths.
 *
 * <p>This filter sits between candidate admission and final weighted ranking so the ranker is
 * not forced to choose only from one narrow scalar score ordering.</p>
 */
final class GraphPathParetoFilter {

    /**
     * Selects enough Pareto layers for the final ranker to work with.
     */
    GraphPathParetoSelection selectForRanking(List<RestoredCandidatePath> candidates, int minimumSelectedCount) {
        if (candidates.isEmpty()) {
            return new GraphPathParetoSelection(List.of(), Map.of(), 0);
        }

        List<RestoredCandidatePath> remaining = new ArrayList<>(candidates);
        List<RestoredCandidatePath> selected = new ArrayList<>();
        Map<RestoredCandidatePath, Integer> layerByPath = new HashMap<>();
        int layer = 1;

        while (!remaining.isEmpty() && selected.size() < minimumSelectedCount) {
            // Pareto layers are accumulated until the ranker has enough diversity to choose
            // from without collapsing everything to the first scalar ordering.
            List<RestoredCandidatePath> front = paretoFront(remaining);
            front.sort(defaultOrder());
            for (RestoredCandidatePath path : front) {
                layerByPath.put(path, layer);
            }
            selected.addAll(front);
            remaining.removeAll(front);
            layer++;
        }

        if (selected.isEmpty()) {
            selected.addAll(remaining);
            selected.sort(defaultOrder());
            selected.forEach(path -> layerByPath.put(path, 1));
            layer = 2;
        }

        return new GraphPathParetoSelection(selected, layerByPath, Math.max(1, layer - 1));
    }

    private List<RestoredCandidatePath> paretoFront(List<RestoredCandidatePath> candidates) {
        List<RestoredCandidatePath> front = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            RestoredCandidatePath candidate = candidates.get(i);
            boolean dominated = false;
            for (int j = 0; j < candidates.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (dominates(candidates.get(j), candidate)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                front.add(candidate);
            }
        }
        return front;
    }

    private boolean dominates(RestoredCandidatePath left, RestoredCandidatePath right) {
        boolean noWorsePrice = left.totalPriceCny() <= right.totalPriceCny();
        boolean noWorseDuration = left.totalDurationMinutes() <= right.totalDurationMinutes();
        boolean noWorseTransfers = left.transferCount() <= right.transferCount();
        boolean noWorseStops = left.totalStops() <= right.totalStops();
        boolean noWorseReliability = left.averageOnTimeRate() >= right.averageOnTimeRate();
        boolean noWorseBaggage = left.baggageIncluded() || !right.baggageIncluded();
        if (!(noWorsePrice
                && noWorseDuration
                && noWorseTransfers
                && noWorseStops
                && noWorseReliability
                && noWorseBaggage)) {
            return false;
        }
        return left.totalPriceCny() < right.totalPriceCny()
                || left.totalDurationMinutes() < right.totalDurationMinutes()
                || left.transferCount() < right.transferCount()
                || left.totalStops() < right.totalStops()
                || left.averageOnTimeRate() > right.averageOnTimeRate()
                || (left.baggageIncluded() && !right.baggageIncluded());
    }

    private Comparator<RestoredCandidatePath> defaultOrder() {
        return Comparator.comparingDouble(RestoredCandidatePath::totalPriceCny)
                .thenComparingInt(RestoredCandidatePath::totalDurationMinutes)
                .thenComparingInt(RestoredCandidatePath::transferCount)
                .thenComparing(Comparator.comparingDouble(RestoredCandidatePath::averageOnTimeRate).reversed());
    }
}

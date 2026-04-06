package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已准入候选路径的 Pareto 分层选择器。
 *
 * <p>该过滤器位于候选准入与最终加权排序之间，避免 ranker 只能基于单一标量序做选择。</p>
 */
final class GraphPathParetoFilter {

    /**
     * 选择足够数量的 Pareto 分层供最终排序使用。
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
            // 按层累积到足够规模，保证 ranker 有足够多样性而非退化到单一标量序。
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

    /**
     * 计算当前候选集的 Pareto 前沿。
     */
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

    /**
     * 判断左侧候选是否支配右侧候选。
     */
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

    /**
     * 默认候选排序规则。
     */
    private Comparator<RestoredCandidatePath> defaultOrder() {
        return Comparator.comparingDouble(RestoredCandidatePath::totalPriceCny)
                .thenComparingInt(RestoredCandidatePath::totalDurationMinutes)
                .thenComparingInt(RestoredCandidatePath::transferCount)
                .thenComparing(Comparator.comparingDouble(RestoredCandidatePath::averageOnTimeRate).reversed());
    }
}

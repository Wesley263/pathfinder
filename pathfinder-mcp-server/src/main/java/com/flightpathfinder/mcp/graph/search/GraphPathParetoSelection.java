package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.List;
import java.util.Map;

/**
 * Pareto 选择结果。
 */
final class GraphPathParetoSelection {

    /** 选中的候选路径列表。 */
    private final List<RestoredCandidatePath> selectedCandidates;
    /** 候选路径对应的 Pareto 层号。 */
    private final Map<RestoredCandidatePath, Integer> paretoLayerByPath;
    /** 结果中最大的 Pareto 层号。 */
    private final int maxLayer;

    /**
     * 构造 Pareto 选择结果。
     *
     * @param selectedCandidates 候选路径列表
     * @param paretoLayerByPath 层号映射
     * @param maxLayer 最大层号
     */
    GraphPathParetoSelection(List<RestoredCandidatePath> selectedCandidates,
                             Map<RestoredCandidatePath, Integer> paretoLayerByPath,
                             int maxLayer) {
        this.selectedCandidates = List.copyOf(selectedCandidates);
        this.paretoLayerByPath = Map.copyOf(paretoLayerByPath);
        this.maxLayer = maxLayer;
    }

    /**
     * 返回选中的候选路径。
     *
     * @return 候选路径列表
     */
    List<RestoredCandidatePath> selectedCandidates() {
        return selectedCandidates;
    }

    /**
        * 返回指定候选路径的 Pareto 层号。
     *
     * @param path 候选路径
     * @return 层号
     */
    int layerOf(RestoredCandidatePath path) {
        return paretoLayerByPath.getOrDefault(path, maxLayer);
    }

    /**
     * 返回最大层号。
     *
     * @return 最大层号
     */
    int maxLayer() {
        return maxLayer;
    }
}

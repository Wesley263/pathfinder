package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.List;
import java.util.Map;

final class GraphPathParetoSelection {

    private final List<RestoredCandidatePath> selectedCandidates;
    private final Map<RestoredCandidatePath, Integer> paretoLayerByPath;
    private final int maxLayer;

    GraphPathParetoSelection(List<RestoredCandidatePath> selectedCandidates,
                             Map<RestoredCandidatePath, Integer> paretoLayerByPath,
                             int maxLayer) {
        this.selectedCandidates = List.copyOf(selectedCandidates);
        this.paretoLayerByPath = Map.copyOf(paretoLayerByPath);
        this.maxLayer = maxLayer;
    }

    List<RestoredCandidatePath> selectedCandidates() {
        return selectedCandidates;
    }

    int layerOf(RestoredCandidatePath path) {
        return paretoLayerByPath.getOrDefault(path, maxLayer);
    }

    int maxLayer() {
        return maxLayer;
    }
}

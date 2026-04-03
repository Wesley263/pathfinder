package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import java.util.List;

record GraphPathSearchNode(
        GraphPathPartialState state,
        List<RestoredGraphEdge> edges,
        double optimisticScore,
        long sequence) {
}

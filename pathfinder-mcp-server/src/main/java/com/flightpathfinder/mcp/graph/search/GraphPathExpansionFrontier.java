package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * 说明。
 *
 * 说明。
 */
final class GraphPathExpansionFrontier {

    /** 注释说明。 */
    private static final Comparator<GraphPathSearchNode> ORDER = Comparator
            .comparingDouble(GraphPathSearchNode::optimisticScore)
            .reversed()
            .thenComparingLong(GraphPathSearchNode::sequence);

    /** 注释说明。 */
    private final NavigableSet<GraphPathSearchNode> nodes = new TreeSet<>(ORDER);

    /**
     * 说明。
     */
    void offer(GraphPathSearchNode node) {
        nodes.add(node);
    }

    /**
     * 说明。
     */
    GraphPathSearchNode pollBest() {
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.pollFirst();
    }

    /**
     * 说明。
     */
    double peekBestScore() {
        if (nodes.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return nodes.first().optimisticScore();
    }

    /**
     * 说明。
     */
    List<GraphPathSearchNode> trimToSize(int maxFrontierSize) {
        List<GraphPathSearchNode> trimmed = new ArrayList<>();
        while (nodes.size() > maxFrontierSize) {
            GraphPathSearchNode removed = nodes.pollLast();
            if (removed != null) {
                trimmed.add(removed);
            }
        }
        return List.copyOf(trimmed);
    }

    /**
     * 说明。
     */
    boolean isEmpty() {
        return nodes.isEmpty();
    }
}

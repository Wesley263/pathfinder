package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Ordered frontier of partial search nodes.
 *
 * <p>This structure is separate from the candidate pool because partial states and completed
 * paths serve different purposes: the frontier controls expansion order, while the candidate
 * pool controls when search can stop.</p>
 */
final class GraphPathExpansionFrontier {

    private static final Comparator<GraphPathSearchNode> ORDER = Comparator
            .comparingDouble(GraphPathSearchNode::optimisticScore)
            .reversed()
            .thenComparingLong(GraphPathSearchNode::sequence);

    private final NavigableSet<GraphPathSearchNode> nodes = new TreeSet<>(ORDER);

    /**
     * Adds a partial search node to the frontier.
     */
    void offer(GraphPathSearchNode node) {
        nodes.add(node);
    }

    /**
     * Removes and returns the current best frontier node.
     */
    GraphPathSearchNode pollBest() {
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.pollFirst();
    }

    /**
     * Returns the optimistic score of the current best frontier node.
     */
    double peekBestScore() {
        if (nodes.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return nodes.first().optimisticScore();
    }

    /**
     * Trims the frontier down to the supplied max size and returns discarded nodes.
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
     * Returns whether the frontier currently holds any search nodes.
     */
    boolean isEmpty() {
        return nodes.isEmpty();
    }
}

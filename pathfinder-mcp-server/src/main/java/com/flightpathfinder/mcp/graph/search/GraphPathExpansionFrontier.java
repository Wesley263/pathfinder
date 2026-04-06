package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * 搜索 frontier 容器。
 *
 * 使用按 optimisticScore 降序的有序集合维护待扩展节点。
 */
final class GraphPathExpansionFrontier {

    /** frontier 排序规则：分数优先，其次序列号稳定排序。 */
    private static final Comparator<GraphPathSearchNode> ORDER = Comparator
            .comparingDouble(GraphPathSearchNode::optimisticScore)
            .reversed()
            .thenComparingLong(GraphPathSearchNode::sequence);

    /** 待扩展节点集合。 */
    private final NavigableSet<GraphPathSearchNode> nodes = new TreeSet<>(ORDER);

    /**
     * 向 frontier 加入节点。
     */
    void offer(GraphPathSearchNode node) {
        nodes.add(node);
    }

    /**
     * 取出当前最优节点。
     */
    GraphPathSearchNode pollBest() {
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.pollFirst();
    }

    /**
        * 查看当前最优分数。
     */
    double peekBestScore() {
        if (nodes.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return nodes.first().optimisticScore();
    }

    /**
        * 将 frontier 裁剪到指定上限并返回被移除节点。
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
     * 判断 frontier 是否为空。
     */
    boolean isEmpty() {
        return nodes.isEmpty();
    }
}

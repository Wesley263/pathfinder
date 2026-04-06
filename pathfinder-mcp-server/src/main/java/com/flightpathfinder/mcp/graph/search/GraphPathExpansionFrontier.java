package com.flightpathfinder.mcp.graph.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * 部分搜索节点的有序 frontier。
 *
 * <p>该结构与候选池分离：frontier 负责扩展顺序，候选池负责收敛与停止判定。</p>
 */
final class GraphPathExpansionFrontier {

    /** frontier 排序规则。 */
    private static final Comparator<GraphPathSearchNode> ORDER = Comparator
            .comparingDouble(GraphPathSearchNode::optimisticScore)
            .reversed()
            .thenComparingLong(GraphPathSearchNode::sequence);

    /** frontier 节点集合。 */
    private final NavigableSet<GraphPathSearchNode> nodes = new TreeSet<>(ORDER);

    /**
     * 向 frontier 添加部分搜索节点。
     */
    void offer(GraphPathSearchNode node) {
        nodes.add(node);
    }

    /**
     * 弹出并返回当前最优 frontier 节点。
     */
    GraphPathSearchNode pollBest() {
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.pollFirst();
    }

    /**
     * 返回当前最优 frontier 节点的乐观分。
     */
    double peekBestScore() {
        if (nodes.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return nodes.first().optimisticScore();
    }

    /**
     * 将 frontier 裁剪到指定上限并返回被裁剪节点。
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

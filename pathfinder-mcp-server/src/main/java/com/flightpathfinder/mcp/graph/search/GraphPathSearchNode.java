package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import java.util.List;

/**
 * 说明。
 *
 * @param state 当前部分状态
 * @param edges 从根到当前节点的边序列
 * @param optimisticScore 乐观估计分
 * @param sequence 序列号（用于稳定排序）
 */
record GraphPathSearchNode(
        GraphPathPartialState state,
        List<RestoredGraphEdge> edges,
        double optimisticScore,
        long sequence) {
}

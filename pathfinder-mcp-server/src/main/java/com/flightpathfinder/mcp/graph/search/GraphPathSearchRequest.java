package com.flightpathfinder.mcp.graph.search;

/**
 * 图路径搜索请求参数。
 *
 * @param graphKey 图标识
 * @param origin 起点机场
 * @param destination 终点机场
 * @param maxBudget 最大预算（人民币）
 * @param stopoverDays 停留天数
 * @param maxSegments 最大航段数
 * @param topK 返回候选数量
 */
public record GraphPathSearchRequest(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}


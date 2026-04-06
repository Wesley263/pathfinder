package com.flightpathfinder.mcp.graph.model;

import java.util.List;

/**
 * 搜索引擎输出的候选路径。
 *
 * @param legs 路径分段列表
 * @param totalPriceCny 路径总价（人民币）
 * @param totalDurationMinutes 路径总时长（分钟）
 * @param totalDistanceKm 路径总距离（公里）
 * @param averageOnTimeRate 平均准点率
 */
public record RestoredCandidatePath(
        List<RestoredPathLeg> legs,
        double totalPriceCny,
        int totalDurationMinutes,
        double totalDistanceKm,
        double averageOnTimeRate) {

    /**
     * 返回航段数。
     *
     * @return 航段数量
     */
    public int segmentCount() {
        return legs.size();
    }

    /**
     * 返回中转次数。
     *
     * @return 中转次数
     */
    public int transferCount() {
        return Math.max(0, legs.size() - 1);
    }

    /**
     * 判断是否为直飞。
     *
     * @return 直飞返回 true
     */
    public boolean isDirect() {
        return legs.size() == 1;
    }

    /**
     * 判断全程是否都含托运行李。
     *
     * @return 全含返回 true
     */
    public boolean baggageIncluded() {
        return legs.stream().allMatch(RestoredPathLeg::baggageIncluded);
    }

    /**
     * 计算总经停次数。
     *
     * @return 总经停次数
     */
    public int totalStops() {
        return legs.stream().mapToInt(RestoredPathLeg::stops).sum();
    }

    /**
     * 计算平均竞争度。
     *
     * @return 平均竞争度
     */
    public double averageCompetitionCount() {
        return legs.stream()
                .mapToInt(RestoredPathLeg::competitionCount)
                .average()
                .orElse(0.0);
    }

    /**
     * 返回中转枢纽机场列表。
     *
     * @return 枢纽机场代码列表
     */
    public List<String> hubAirports() {
        if (legs.size() <= 1) {
            return List.of();
        }
        return legs.stream()
                .limit(legs.size() - 1L)
                .map(RestoredPathLeg::destination)
                .toList();
    }
}

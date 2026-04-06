package com.flightpathfinder.mcp.graph.model;

/**
 * 内存图中的边定义。
 *
 * @param edgeId 边唯一标识
 * @param origin 起点机场
 * @param destination 终点机场
 * @param carrierCode 承运方代码
 * @param carrierName 承运方名称
 * @param carrierType 承运方类型
 * @param basePriceCny 基础价格（人民币）
 * @param durationMinutes 时长（分钟）
 * @param distanceKm 距离（公里）
 * @param onTimeRate 准点率
 * @param baggageIncluded 是否含托运行李
 * @param competitionCount 竞争度
 * @param stops 经停次数
 */
public record RestoredGraphEdge(
        String edgeId,
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops) {
}


package com.flightpathfinder.mcp.graph.model;

/**
 * 候选路径中的单段航程。
 *
 * @param origin 起点机场
 * @param destination 终点机场
 * @param carrierCode 承运方代码
 * @param carrierName 承运方名称
 * @param carrierType 承运方类型
 * @param priceCny 价格（人民币）
 * @param durationMinutes 时长（分钟）
 * @param distanceKm 距离（公里）
 * @param onTimeRate 准点率
 * @param baggageIncluded 是否含托运行李
 * @param competitionCount 竞争度
 * @param stops 经停次数
 */
public record RestoredPathLeg(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops) {
}

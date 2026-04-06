package com.flightpathfinder.framework.readmodel.graph;

import java.util.Map;

/**
 * 图快照中的有向边定义。
 *
 * @param edgeId 边唯一标识
 * @param fromNodeId 起点节点标识
 * @param toNodeId 终点节点标识
 * @param carrierCode 承运方代码
 * @param carrierName 承运方名称
 * @param carrierType 承运方类型
 * @param basePriceCny 基础价格（人民币）
 * @param durationMinutes 时长（分钟）
 * @param distanceKm 距离（公里）
 * @param onTimeRate 准点率
 * @param baggageIncluded 是否含托运行李
 * @param competitionCount 竞争度指标
 * @param stops 经停次数
 * @param attributes 扩展属性
 */
public record GraphSnapshotEdge(
        String edgeId,
        String fromNodeId,
        String toNodeId,
        String carrierCode,
        String carrierName,
        String carrierType,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        double onTimeRate,
        boolean baggageIncluded,
        int competitionCount,
        int stops,
        Map<String, Object> attributes) {
}

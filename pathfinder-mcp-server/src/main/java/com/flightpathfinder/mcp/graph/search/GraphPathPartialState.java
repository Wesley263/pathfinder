package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import java.util.HashSet;
import java.util.Set;

/**
 * 路径搜索中的部分状态。
 *
 * <p>该状态用于 frontier 扩展过程，记录当前机场、已访问节点与累计代价。</p>
 */
final class GraphPathPartialState {

    /** 当前所在机场。 */
    private final String currentAirport;
    /** 已访问机场集合。 */
    private final Set<String> visitedAirports;
    /** 已使用航段数。 */
    private final int segmentsUsed;
    /** 累计价格（人民币）。 */
    private final double totalPriceCny;
    /** 累计时长（分钟）。 */
    private final int totalDurationMinutes;
    /** 累计距离（公里）。 */
    private final double totalDistanceKm;
    /** 准点率累计和。 */
    private final double onTimeRateSum;
    /** 竞争度累计和。 */
    private final int competitionCountSum;
    /** 累计经停次数。 */
    private final int totalStops;
    /** 是否全程含托运行李。 */
    private final boolean baggageIncluded;

    /**
     * 构造部分状态。
     */
    private GraphPathPartialState(String currentAirport,
                                  Set<String> visitedAirports,
                                  int segmentsUsed,
                                  double totalPriceCny,
                                  int totalDurationMinutes,
                                  double totalDistanceKm,
                                  double onTimeRateSum,
                                  int competitionCountSum,
                                  int totalStops,
                                  boolean baggageIncluded) {
        this.currentAirport = currentAirport;
        this.visitedAirports = Set.copyOf(visitedAirports);
        this.segmentsUsed = segmentsUsed;
        this.totalPriceCny = totalPriceCny;
        this.totalDurationMinutes = totalDurationMinutes;
        this.totalDistanceKm = totalDistanceKm;
        this.onTimeRateSum = onTimeRateSum;
        this.competitionCountSum = competitionCountSum;
        this.totalStops = totalStops;
        this.baggageIncluded = baggageIncluded;
    }

    /**
     * 创建根状态。
     *
     * @param originAirport 起点机场
     * @return 根状态
     */
    static GraphPathPartialState root(String originAirport) {
        return new GraphPathPartialState(
                originAirport,
                Set.of(originAirport),
                0,
                0.0,
                0,
                0.0,
                0.0,
                0,
                0,
                true);
    }

    /**
     * 基于一条边推进到下一个状态。
     *
     * @param edge 当前扩展边
     * @param transferMinutes 中转时间
     * @param stopoverMinutes 停留时间
     * @return 新状态
     */
    GraphPathPartialState advance(RestoredGraphEdge edge, int transferMinutes, int stopoverMinutes) {
        Set<String> nextVisitedAirports = new HashSet<>(visitedAirports);
        nextVisitedAirports.add(edge.destination());
        int additionalDuration = edge.durationMinutes();
        if (segmentsUsed > 0) {
            additionalDuration += transferMinutes + stopoverMinutes;
        }
        return new GraphPathPartialState(
                edge.destination(),
                nextVisitedAirports,
                segmentsUsed + 1,
                totalPriceCny + edge.basePriceCny(),
                totalDurationMinutes + additionalDuration,
                totalDistanceKm + edge.distanceKm(),
                onTimeRateSum + edge.onTimeRate(),
                competitionCountSum + edge.competitionCount(),
                totalStops + edge.stops(),
                baggageIncluded && edge.baggageIncluded());
    }

    /** @return 当前机场 */
    String currentAirport() {
        return currentAirport;
    }

    /** @return 已访问机场集合 */
    Set<String> visitedAirports() {
        return visitedAirports;
    }

    /** @return 已使用航段数 */
    int segmentsUsed() {
        return segmentsUsed;
    }

    /** @return 累计价格 */
    double totalPriceCny() {
        return totalPriceCny;
    }

    /** @return 累计时长 */
    int totalDurationMinutes() {
        return totalDurationMinutes;
    }

    /** @return 累计距离 */
    double totalDistanceKm() {
        return totalDistanceKm;
    }

    /**
     * 计算平均准点率。
     *
     * @return 平均准点率
     */
    double averageOnTimeRate() {
        if (segmentsUsed == 0) {
            return 0.0;
        }
        return onTimeRateSum / segmentsUsed;
    }

    /**
     * 计算平均竞争度。
     *
     * @return 平均竞争度
     */
    double averageCompetitionCount() {
        if (segmentsUsed == 0) {
            return 0.0;
        }
        return (double) competitionCountSum / segmentsUsed;
    }

    /** @return 累计经停次数 */
    int totalStops() {
        return totalStops;
    }

    /** @return 是否全程含托运行李 */
    boolean baggageIncluded() {
        return baggageIncluded;
    }
}

package com.flightpathfinder.mcp.pricelookup;

/**
 * 单个城市对的最低价比价结果项。
 *
 * @param cityPair 城市对键（{@code ORG,DST}）
 * @param origin 出发机场 IATA 三字码
 * @param destination 到达机场 IATA 三字码
 * @param airlineCode 航司 IATA 代码
 * @param airlineName 航司名称
 * @param airlineType 航司类型
 * @param date 出行日期（yyyy-MM-dd）
 * @param lowestPriceCny 最低价（人民币）
 * @param basePriceCny 基准价（人民币）
 * @param durationMinutes 时长（分钟）
 * @param distanceKm 距离（公里）
 * @param lowCostCarrier 是否低成本航司
 */
public record PriceLookupItem(
        String cityPair,
        String origin,
        String destination,
        String airlineCode,
        String airlineName,
        String airlineType,
        String date,
        double lowestPriceCny,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        boolean lowCostCarrier) {
}

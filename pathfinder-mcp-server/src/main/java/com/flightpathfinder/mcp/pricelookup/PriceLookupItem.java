package com.flightpathfinder.mcp.pricelookup;

/**
 * 单个城市对的最低价比价结果项。
 *
 * @param cityPair 参数说明。
 * @param origin 参数说明。
 * @param destination 参数说明。
 * @param airlineCode 参数说明。
 * @param airlineName 航司名称
 * @param airlineType 航司类型
 * @param date 参数说明。
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

package com.flightpathfinder.mcp.flightsearch;

/**
 * 直飞检索候选项。
 *
 * @param airlineCode 参数说明。
 * @param airlineName 承运航司名称
 * @param airlineType 参数说明。
 * @param origin 参数说明。
 * @param destination 参数说明。
 * @param date 参数说明。
 * @param priceCny 动态估算票价（人民币）
 * @param basePriceCny 基准票价（人民币）
 * @param durationMinutes 航程时长（分钟）
 * @param distanceKm 航程距离（公里）
 * @param lowCostCarrier 是否低成本航司
 */
public record FlightSearchOption(
        String airlineCode,
        String airlineName,
        String airlineType,
        String origin,
        String destination,
        String date,
        double priceCny,
        double basePriceCny,
        int durationMinutes,
        double distanceKm,
        boolean lowCostCarrier) {
}

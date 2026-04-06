package com.flightpathfinder.mcp.flightsearch;

/**
 * 直飞检索候选项。
 *
 * @param airlineCode 承运航司 IATA 代码
 * @param airlineName 承运航司名称
 * @param airlineType 航司类型（如 FSC/LCC/MID）
 * @param origin 出发机场 IATA 三字码
 * @param destination 到达机场 IATA 三字码
 * @param date 出行日期（yyyy-MM-dd）
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

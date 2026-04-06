package com.flightpathfinder.mcp.citycost;

/**
 * 单个城市成本条目。
 *
 * @param iataCode 城市或机场 IATA 三字码
 * @param city 城市名称
 * @param country 国家名称
 * @param countryCode 国家代码
 * @param dailyCostUsd 日均成本（美元）
 * @param accommodationUsd 住宿成本（美元）
 * @param mealCostUsd 餐饮成本（美元）
 * @param transportationUsd 交通成本（美元）
 */
public record CityCostItem(
        String iataCode,
        String city,
        String country,
        String countryCode,
        double dailyCostUsd,
        double accommodationUsd,
        double mealCostUsd,
        double transportationUsd) {
}

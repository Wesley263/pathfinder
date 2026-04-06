package com.flightpathfinder.mcp.citycost;

import java.util.List;

/**
 * 城市成本查询结果。
 *
 * @param requestedCities 请求的城市代码列表
 * @param items 命中的城市成本条目
 * @param missingCities 缺失城市代码列表
 */
public record CityCostResult(
        List<String> requestedCities,
        List<CityCostItem> items,
        List<String> missingCities) {
}

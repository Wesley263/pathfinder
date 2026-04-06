package com.flightpathfinder.mcp.flightsearch;

import java.time.LocalDate;
import java.util.Locale;

/**
 * 直飞检索查询参数。
 *
 * @param origin 出发机场 IATA 三字码
 * @param destination 到达机场 IATA 三字码
 * @param date 查询基准日期
 * @param flexibilityDays 日期弹性天数
 * @param topK 返回候选上限
 */
public record FlightSearchQuery(
        String origin,
        String destination,
        LocalDate date,
        int flexibilityDays,
        int topK) {

    /**
     * 构造时统一完成机场代码归一化与必要字段校验。
     */
    public FlightSearchQuery {
        origin = normalizeCode(origin);
        destination = normalizeCode(destination);
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

package com.flightpathfinder.mcp.riskevaluate;

import java.util.Locale;

/**
 * 中转风险评估请求。
 *
 * @param hubAirport 参数说明。
 * @param firstAirline 第一段航司代码
 * @param secondAirline 第二段航司代码
 * @param bufferHours 中转缓冲时长（小时）
 */
public record RiskEvaluateQuery(
        String hubAirport,
        String firstAirline,
        String secondAirline,
        double bufferHours) {

    /**
     * 构造时完成机场/航司代码归一化与缓冲时长校验。
     */
    public RiskEvaluateQuery {
        hubAirport = normalizeAirport(hubAirport);
        firstAirline = normalizeAirline(firstAirline);
        secondAirline = normalizeAirline(secondAirline);
        if (bufferHours <= 0D) {
            throw new IllegalArgumentException("bufferHours must be greater than 0");
        }
    }

    private static String normalizeAirport(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeAirline(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

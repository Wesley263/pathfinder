package com.flightpathfinder.mcp.citycost;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 城市成本查询请求。
 *
 * @param iataCodes 参数说明。
 */
public record CityCostQuery(List<String> iataCodes) {

    /**
     * 构造时完成代码归一化、去重与必填校验。
     */
    public CityCostQuery {
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        for (String iataCode : iataCodes == null ? List.<String>of() : iataCodes) {
            String normalized = normalize(iataCode);
            if (!normalized.isBlank()) {
                normalizedCodes.add(normalized);
            }
        }
        iataCodes = List.copyOf(normalizedCodes);
        if (iataCodes.isEmpty()) {
            throw new IllegalArgumentException("iataCodes is required");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

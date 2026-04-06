package com.flightpathfinder.mcp.visacheck;

import java.util.List;
import java.util.Locale;

/**
 * 签证核验查询请求。
 *
 * @param countryCodes 参数说明。
 * @param stayDays 计划停留天数
 * @param passportCountry 参数说明。
 */
public record VisaCheckQuery(
        List<String> countryCodes,
        int stayDays,
        String passportCountry) {

    /**
     * 构造时完成国家代码归一化、去重及必填校验。
     */
    public VisaCheckQuery {
        countryCodes = List.copyOf(countryCodes == null ? List.of() : countryCodes.stream()
                .map(VisaCheckQuery::normalizeCode)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList());
        passportCountry = normalizeCode(passportCountry);
        if (countryCodes.isEmpty()) {
            throw new IllegalArgumentException("countryCodes must contain at least one country");
        }
        if (passportCountry.isBlank()) {
            throw new IllegalArgumentException("passportCountry is required");
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

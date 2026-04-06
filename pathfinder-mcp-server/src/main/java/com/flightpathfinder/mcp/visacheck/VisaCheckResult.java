package com.flightpathfinder.mcp.visacheck;

import java.util.List;

/**
 * 签证核验聚合结果。
 *
 * @param passportCountry 护照国家代码
 * @param stayDays 计划停留天数
 * @param items 各目的地国家的签证结果项
 */
public record VisaCheckResult(
        String passportCountry,
        int stayDays,
        List<VisaCheckItem> items) {

    /**
     * 构造时完成字符串规整与结果集合不可变拷贝。
     */
    public VisaCheckResult {
        passportCountry = passportCountry == null ? "" : passportCountry.trim();
        items = List.copyOf(items == null ? List.of() : items);
    }
}

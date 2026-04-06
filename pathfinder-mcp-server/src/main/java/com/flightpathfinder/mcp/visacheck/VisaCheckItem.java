package com.flightpathfinder.mcp.visacheck;

/**
 * 单个国家的签证核验结果项。
 *
 * @param countryCode 参数说明。
 * @param countryName 国家名称（或回退代码）
 * @param visaStatus 参数说明。
 * @param maxStayDays 允许停留天数，未知时可为负值
 * @param transitFree 是否支持过境免签
 * @param transitMaxHours 过境免签最大时长（小时），未知可为空
 * @param notes 附加说明
 */
public record VisaCheckItem(
        String countryCode,
        String countryName,
        String visaStatus,
        int maxStayDays,
        boolean transitFree,
        Integer transitMaxHours,
        String notes) {
}

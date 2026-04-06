package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminMcpToolSummaryVO(
        String toolId,
        String displayName,
        String description,
        String source,
        boolean available,
        String availabilityMessage,
        String dependencySummary,
        String dependencyStatus) {
}


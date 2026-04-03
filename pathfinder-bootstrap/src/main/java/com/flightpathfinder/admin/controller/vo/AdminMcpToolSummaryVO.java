package com.flightpathfinder.admin.controller.vo;

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

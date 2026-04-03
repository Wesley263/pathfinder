package com.flightpathfinder.admin.service;

public record AdminMcpToolSummaryItem(
        String toolId,
        String displayName,
        String description,
        String source,
        boolean available,
        String availabilityMessage,
        String dependencySummary,
        String dependencyStatus) {

    public AdminMcpToolSummaryItem {
        toolId = toolId == null ? "" : toolId.trim();
        displayName = displayName == null ? "" : displayName.trim();
        description = description == null ? "" : description.trim();
        source = source == null ? "" : source.trim();
        availabilityMessage = availabilityMessage == null ? "" : availabilityMessage.trim();
        dependencySummary = dependencySummary == null ? "" : dependencySummary.trim();
        dependencyStatus = dependencyStatus == null ? "" : dependencyStatus.trim();
    }
}

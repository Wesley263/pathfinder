package com.flightpathfinder.admin.service;

import java.util.List;
import java.util.Map;

public record AdminMcpToolDetail(
        String toolId,
        String displayName,
        String description,
        String source,
        boolean available,
        String availabilityMessage,
        String dependencySummary,
        String dependencyStatus,
        List<String> dependencyDetails,
        List<String> statusHints,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        String remoteBaseUrl) {

    public AdminMcpToolDetail {
        toolId = toolId == null ? "" : toolId.trim();
        displayName = displayName == null ? "" : displayName.trim();
        description = description == null ? "" : description.trim();
        source = source == null ? "" : source.trim();
        availabilityMessage = availabilityMessage == null ? "" : availabilityMessage.trim();
        dependencySummary = dependencySummary == null ? "" : dependencySummary.trim();
        dependencyStatus = dependencyStatus == null ? "" : dependencyStatus.trim();
        dependencyDetails = List.copyOf(dependencyDetails == null ? List.of() : dependencyDetails);
        statusHints = List.copyOf(statusHints == null ? List.of() : statusHints);
        inputSchema = Map.copyOf(inputSchema == null ? Map.of() : inputSchema);
        outputSchema = Map.copyOf(outputSchema == null ? Map.of() : outputSchema);
        remoteBaseUrl = remoteBaseUrl == null ? "" : remoteBaseUrl.trim();
    }
}

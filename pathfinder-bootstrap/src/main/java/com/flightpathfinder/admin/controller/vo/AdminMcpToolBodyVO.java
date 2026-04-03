package com.flightpathfinder.admin.controller.vo;

import java.util.List;
import java.util.Map;

public record AdminMcpToolBodyVO(
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
}

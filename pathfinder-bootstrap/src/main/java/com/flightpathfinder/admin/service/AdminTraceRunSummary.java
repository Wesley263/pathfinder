package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

public record AdminTraceRunSummary(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        boolean partial,
        boolean errorOccurred,
        Instant startedAt,
        Instant finishedAt,
        int nodeCount,
        int toolCount,
        List<AdminTraceNodeSummary> stages,
        List<AdminTraceToolSummary> mcpToolSummaries) {

    public AdminTraceRunSummary {
        traceId = traceId == null ? "" : traceId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        scene = scene == null ? "" : scene.trim();
        overallStatus = overallStatus == null || overallStatus.isBlank() ? "UNKNOWN" : overallStatus.trim();
        nodeCount = Math.max(0, nodeCount);
        toolCount = Math.max(0, toolCount);
        stages = List.copyOf(stages == null ? List.of() : stages);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }

    public static AdminTraceRunSummary empty() {
        return new AdminTraceRunSummary(
                "",
                "",
                "",
                "",
                "UNKNOWN",
                false,
                false,
                false,
                null,
                null,
                0,
                0,
                List.of(),
                List.of());
    }
}

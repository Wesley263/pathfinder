package com.flightpathfinder.rag.core.trace;

import java.time.Instant;

public record RagTraceRunSummary(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        Instant startedAt,
        Instant finishedAt,
        int nodeCount,
        int toolCount) {

    public RagTraceRunSummary {
        traceId = traceId == null ? "" : traceId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        scene = scene == null ? "" : scene.trim();
        overallStatus = overallStatus == null || overallStatus.isBlank() ? "UNKNOWN" : overallStatus.trim();
        nodeCount = Math.max(0, nodeCount);
        toolCount = Math.max(0, toolCount);
    }
}

package com.flightpathfinder.rag.controller.vo;

import java.time.Instant;

public record RagTraceRunVO(
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
}

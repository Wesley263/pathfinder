package com.flightpathfinder.rag.core.trace.repository;

import java.time.Instant;

public record PersistedRagTraceRunRecord(
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

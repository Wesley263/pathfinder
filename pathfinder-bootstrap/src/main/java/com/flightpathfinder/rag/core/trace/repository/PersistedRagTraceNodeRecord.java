package com.flightpathfinder.rag.core.trace.repository;

import java.time.Instant;

public record PersistedRagTraceNodeRecord(
        String traceId,
        int nodeIndex,
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        String attributesJson) {
}

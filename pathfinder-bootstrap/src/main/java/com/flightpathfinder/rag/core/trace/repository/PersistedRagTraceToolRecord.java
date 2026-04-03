package com.flightpathfinder.rag.core.trace.repository;

public record PersistedRagTraceToolRecord(
        String traceId,
        int toolIndex,
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {
}

package com.flightpathfinder.rag.core.trace;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record RagTraceStageResult(
        String stageName,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {

    public RagTraceStageResult {
        stageName = stageName == null ? "" : stageName.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        summary = summary == null ? "" : summary.trim();
        startedAt = startedAt == null ? Instant.now() : startedAt;
        finishedAt = finishedAt == null ? startedAt : finishedAt;
        attributes = Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
}

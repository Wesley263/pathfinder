package com.flightpathfinder.rag.core.trace;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record RagTraceNodeDetail(
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {

    public RagTraceNodeDetail {
        nodeName = nodeName == null ? "" : nodeName.trim();
        nodeType = nodeType == null || nodeType.isBlank() ? "INTERNAL" : nodeType.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        summary = summary == null ? "" : summary.trim();
        attributes = Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
}

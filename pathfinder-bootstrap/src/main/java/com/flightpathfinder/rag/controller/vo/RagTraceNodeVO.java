package com.flightpathfinder.rag.controller.vo;

import java.time.Instant;
import java.util.Map;

public record RagTraceNodeVO(
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {
}

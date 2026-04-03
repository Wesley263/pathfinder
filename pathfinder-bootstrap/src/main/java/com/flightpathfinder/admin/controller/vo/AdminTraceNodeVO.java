package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.Map;

public record AdminTraceNodeVO(
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        boolean partial,
        boolean snapshotMiss,
        String error,
        Map<String, Object> attributes) {
}

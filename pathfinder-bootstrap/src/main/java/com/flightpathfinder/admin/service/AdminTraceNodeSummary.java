package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端服务层数据模型。
 */
public record AdminTraceNodeSummary(
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

    public AdminTraceNodeSummary {
        nodeName = nodeName == null ? "" : nodeName.trim();
        nodeType = nodeType == null || nodeType.isBlank() ? "INTERNAL" : nodeType.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        summary = summary == null ? "" : summary.trim();
        error = error == null ? "" : error.trim();
        attributes = Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
}


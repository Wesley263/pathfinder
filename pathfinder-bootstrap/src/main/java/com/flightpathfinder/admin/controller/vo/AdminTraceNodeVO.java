package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.Map;

/**
 * 管理端响应视图模型。
 */
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


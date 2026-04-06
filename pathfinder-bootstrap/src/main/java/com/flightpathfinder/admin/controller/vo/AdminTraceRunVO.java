package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

/**
 * 管理端响应视图模型。
 */
public record AdminTraceRunVO(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        boolean partial,
        boolean errorOccurred,
        Instant startedAt,
        Instant finishedAt,
        int nodeCount,
        int toolCount,
        List<AdminTraceNodeVO> stages,
        List<AdminTraceToolVO> mcpToolSummaries) {
}


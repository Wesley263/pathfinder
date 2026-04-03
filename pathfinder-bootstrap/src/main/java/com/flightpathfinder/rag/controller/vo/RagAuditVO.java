package com.flightpathfinder.rag.controller.vo;

import java.util.List;

public record RagAuditVO(
        boolean stageOneCompleted,
        String retrievalStatus,
        String answerStatus,
        boolean snapshotMissAffected,
        String requestId,
        String conversationId,
        String traceId,
        String traceStatus,
        List<RagTraceStageVO> traceStages,
        List<RagTraceToolVO> mcpToolSummaries) {
}

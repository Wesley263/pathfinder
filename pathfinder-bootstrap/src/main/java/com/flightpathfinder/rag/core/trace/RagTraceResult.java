package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.List;

public record RagTraceResult(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        TraceRoot root,
        List<RagTraceStageResult> stages,
        List<RagTraceToolSummary> mcpToolSummaries) {

    public RagTraceResult {
        traceId = traceId == null ? "" : traceId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        scene = scene == null ? "" : scene.trim();
        overallStatus = overallStatus == null || overallStatus.isBlank() ? "UNKNOWN" : overallStatus.trim();
        stages = List.copyOf(stages == null ? List.of() : stages);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }
}

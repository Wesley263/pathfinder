package com.flightpathfinder.rag.core.trace;

public record RagTraceToolSummary(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {

    public RagTraceToolSummary {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
    }
}

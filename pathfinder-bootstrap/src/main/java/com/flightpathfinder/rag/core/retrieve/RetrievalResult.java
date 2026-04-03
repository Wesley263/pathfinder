package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

public record RetrievalResult(
        String status,
        String summary,
        StageOneRagResult stageOneResult,
        KbContext kbContext,
        McpContext mcpContext) {

    public RetrievalResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        stageOneResult = stageOneResult == null ? new StageOneRagResult(null, null, null, null) : stageOneResult;
        kbContext = kbContext == null ? KbContext.empty("no KB retrieval executed", java.util.List.of()) : kbContext;
        mcpContext = mcpContext == null ? McpContext.skipped("no MCP execution executed") : mcpContext;
    }

    public boolean isEmpty() {
        return kbContext.empty() && mcpContext.executions().isEmpty();
    }

    public boolean hasSnapshotMiss() {
        return mcpContext.hasSnapshotMiss();
    }
}

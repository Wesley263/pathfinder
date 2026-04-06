package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段总结果。
 *
 * 说明。
 * 说明。
 *
 * @param status 参数说明。
 * @param summary 参数说明。
 * @param stageOneResult 参数说明。
 * @param kbContext 参数说明。
 * @param mcpContext 参数说明。
 */
public record RetrievalResult(
        String status,
        String summary,
        StageOneRagResult stageOneResult,
        KbContext kbContext,
        McpContext mcpContext) {

    /**
     * 说明。
     */
    public RetrievalResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        stageOneResult = stageOneResult == null ? new StageOneRagResult(null, null, null, null) : stageOneResult;
        kbContext = kbContext == null ? KbContext.empty("no KB retrieval executed", java.util.List.of()) : kbContext;
        mcpContext = mcpContext == null ? McpContext.skipped("no MCP execution executed") : mcpContext;
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    public boolean isEmpty() {
        return kbContext.empty() && mcpContext.executions().isEmpty();
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    public boolean hasSnapshotMiss() {
        return mcpContext.hasSnapshotMiss();
    }
}


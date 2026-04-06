package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段总结果。
 *
 * 聚合 stage one 输出、KB 上下文与 MCP 上下文。
 *
 * @param status 检索阶段状态
 * @param summary 检索摘要
 * @param stageOneResult 第一阶段输出快照
 * @param kbContext KB 检索上下文
 * @param mcpContext MCP 执行上下文
 */
public record RetrievalResult(
        String status,
        String summary,
        StageOneRagResult stageOneResult,
        KbContext kbContext,
        McpContext mcpContext) {

    /**
        * 归一化构造参数，避免空引用。
     */
    public RetrievalResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        stageOneResult = stageOneResult == null ? new StageOneRagResult(null, null, null, null) : stageOneResult;
        kbContext = kbContext == null ? KbContext.empty("no KB retrieval executed", java.util.List.of()) : kbContext;
        mcpContext = mcpContext == null ? McpContext.skipped("no MCP execution executed") : mcpContext;
    }

    /**
     * 判断检索结果是否为空。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return kbContext.empty() && mcpContext.executions().isEmpty();
    }

    /**
     * 判断是否包含图快照缺失。
     *
     * @return 是否存在快照缺失
     */
    public boolean hasSnapshotMiss() {
        return mcpContext.hasSnapshotMiss();
    }
}


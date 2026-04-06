package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段总结果。
 *
 * <p>它把 stage one 输入、KB 上下文和 MCP 上下文收口为统一结果对象，
 * 供 final answer、trace 和 API 审计层继续消费。</p>
 *
 * @param status retrieval 总体状态
 * @param summary 面向审计和 trace 的摘要
 * @param stageOneResult 产生本次 retrieval 的第一阶段结果
 * @param kbContext KB 分支结果
 * @param mcpContext MCP 分支结果
 */
public record RetrievalResult(
        String status,
        String summary,
        StageOneRagResult stageOneResult,
        KbContext kbContext,
        McpContext mcpContext) {

    /**
     * 归一化 retrieval 结果。
     */
    public RetrievalResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        stageOneResult = stageOneResult == null ? new StageOneRagResult(null, null, null, null) : stageOneResult;
        kbContext = kbContext == null ? KbContext.empty("no KB retrieval executed", java.util.List.of()) : kbContext;
        mcpContext = mcpContext == null ? McpContext.skipped("no MCP execution executed") : mcpContext;
    }

    /**
     * 判断 retrieval 阶段是否没有产生任何可用上下文。
     *
     * @return KB 为空且 MCP 未执行时返回 true
     */
    public boolean isEmpty() {
        return kbContext.empty() && mcpContext.executions().isEmpty();
    }

    /**
     * 判断 retrieval 阶段是否受到 SNAPSHOT_MISS 影响。
     *
     * @return 若 MCP 分支出现 snapshot miss，则返回 true
     */
    public boolean hasSnapshotMiss() {
        return mcpContext.hasSnapshotMiss();
    }
}


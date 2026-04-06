package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

/**
 * MCP 执行聚合上下文。
 *
 * 用于承接 retrieval 阶段的 MCP 调用结果，并向回答阶段暴露统一状态。
 *
 * @param status 聚合状态，例如 SUCCESS、PARTIAL_FAILURE、SNAPSHOT_MISS
 * @param summary 执行摘要
 * @param matchedIntents 本次命中的 MCP 意图列表
 * @param executions 每个工具调用的执行记录
 */
public record McpContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<McpExecutionRecord> executions) {

    /**
     * 归一化构造参数，避免空引用。
     */
    public McpContext {
        status = status == null || status.isBlank() ? "SKIPPED" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        executions = List.copyOf(executions == null ? List.of() : executions);
    }

    /**
     * 判断是否存在失败语义的执行记录。
     *
     * @return 是否存在失败
     */
    public boolean hasErrors() {
        return executions.stream().anyMatch(execution -> switch (execution.status()) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "LOW", "MEDIUM", "HIGH" -> false;
            default -> true;
        });
    }

    /**
     * 判断是否存在图快照缺失。
     *
     * @return 是否存在快照缺失
     */
    public boolean hasSnapshotMiss() {
        return executions.stream().anyMatch(McpExecutionRecord::snapshotMiss);
    }

    /**
     * 创建跳过态上下文。
     *
     * @param summary 跳过原因说明
     * @return 跳过态上下文
     */
    public static McpContext skipped(String summary) {
        return new McpContext("SKIPPED", summary, List.of(), List.of());
    }
}


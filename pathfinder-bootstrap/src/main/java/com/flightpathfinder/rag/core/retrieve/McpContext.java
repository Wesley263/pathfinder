package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

/**
 * 工具分支（MCP）的结构化执行结果。
 *
 * <p>它保留命中的 MCP intents 与逐条工具执行记录，方便 retrieval、answer、trace 和 admin 统一消费。</p>
 *
 * @param status 当前 MCP 分支状态
 * @param summary 面向审计和 trace 的摘要
 * @param matchedIntents 本次实际触发的 MCP intents
 * @param executions 每个工具调用的执行记录
 */
public record McpContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<McpExecutionRecord> executions) {

    /**
     * 归一化 MCP 上下文。
     */
    public McpContext {
        status = status == null || status.isBlank() ? "SKIPPED" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        executions = List.copyOf(executions == null ? List.of() : executions);
    }

    /**
     * 判断当前 MCP 分支是否出现非业务成功类错误。
     *
     * @return 如果存在失败记录则返回 true
     */
    public boolean hasErrors() {
        return executions.stream().anyMatch(execution -> switch (execution.status()) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "LOW", "MEDIUM", "HIGH" -> false;
            default -> true;
        });
    }

    /**
     * 判断当前 MCP 分支是否出现 SNAPSHOT_MISS。
     *
     * @return 如果任一执行记录命中 snapshot miss，则返回 true
     */
    public boolean hasSnapshotMiss() {
        return executions.stream().anyMatch(McpExecutionRecord::snapshotMiss);
    }

    /**
     * 创建未执行的 MCP 结果。
     *
     * @param summary 跳过原因说明
     * @return 跳过状态的 MCP 上下文
     */
    public static McpContext skipped(String summary) {
        return new McpContext("SKIPPED", summary, List.of(), List.of());
    }
}


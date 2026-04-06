package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

/**
 * 说明。
 *
 * 说明。
 *
 * @param status 参数说明。
 * @param summary 参数说明。
 * @param matchedIntents 参数说明。
 * @param executions 每个工具调用的执行记录
 */
public record McpContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<McpExecutionRecord> executions) {

    /**
     * 说明。
     */
    public McpContext {
        status = status == null || status.isBlank() ? "SKIPPED" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        executions = List.copyOf(executions == null ? List.of() : executions);
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    public boolean hasErrors() {
        return executions.stream().anyMatch(execution -> switch (execution.status()) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "LOW", "MEDIUM", "HIGH" -> false;
            default -> true;
        });
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    public boolean hasSnapshotMiss() {
        return executions.stream().anyMatch(McpExecutionRecord::snapshotMiss);
    }

    /**
     * 说明。
     *
     * @param summary 跳过原因说明
     * @return 返回结果。
     */
    public static McpContext skipped(String summary) {
        return new McpContext("SKIPPED", summary, List.of(), List.of());
    }
}


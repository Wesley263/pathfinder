package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

public record McpContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<McpExecutionRecord> executions) {

    public McpContext {
        status = status == null || status.isBlank() ? "SKIPPED" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        executions = List.copyOf(executions == null ? List.of() : executions);
    }

    public boolean hasErrors() {
        return executions.stream().anyMatch(execution -> switch (execution.status()) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "LOW", "MEDIUM", "HIGH" -> false;
            default -> true;
        });
    }

    public boolean hasSnapshotMiss() {
        return executions.stream().anyMatch(McpExecutionRecord::snapshotMiss);
    }

    public static McpContext skipped(String summary) {
        return new McpContext("SKIPPED", summary, List.of(), List.of());
    }
}

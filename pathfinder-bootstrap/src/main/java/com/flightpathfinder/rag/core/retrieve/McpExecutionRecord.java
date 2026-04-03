package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import java.util.LinkedHashMap;
import java.util.Map;

public record McpExecutionRecord(
        String intentId,
        String intentName,
        String question,
        String toolId,
        String invocationMode,
        Map<String, Object> requestParameters,
        boolean success,
        String status,
        String message,
        String error,
        boolean snapshotMiss,
        Boolean retryable,
        String suggestedAction,
        McpToolCallResult toolResult) {

    public McpExecutionRecord {
        intentId = intentId == null ? "" : intentId.trim();
        intentName = intentName == null ? "" : intentName.trim();
        question = question == null ? "" : question.trim();
        toolId = toolId == null ? "" : toolId.trim();
        invocationMode = invocationMode == null || invocationMode.isBlank() ? "NONE" : invocationMode;
        requestParameters = Map.copyOf(new LinkedHashMap<>(requestParameters == null ? Map.of() : requestParameters));
        status = status == null || status.isBlank() ? "UNKNOWN" : status;
        message = message == null ? "" : message.trim();
        error = error == null ? "" : error.trim();
        suggestedAction = suggestedAction == null ? "" : suggestedAction.trim();
    }
}

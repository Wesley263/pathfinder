package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 *
 * @param intentId 触发本次调用的意图标识
 * @param intentName 触发本次调用的意图名称
 * @param question 对应的子问题文本
 * @param toolId 参数说明。
 * @param invocationMode 调用模式，例如本地、远端或未调用
 * @param requestParameters 参数说明。
 * @param success 参数说明。
 * @param status 参数说明。
 * @param message 工具返回的业务说明
 * @param error 工具返回的错误信息
 * @param snapshotMiss 是否命中图快照缺失
 * @param retryable 是否可重试
 * @param suggestedAction 参数说明。
 * @param toolResult 参数说明。
 */
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

    /**
     * 说明。
     */
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

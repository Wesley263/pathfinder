package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次 MCP 工具执行记录。
 *
 * <p>它是 retrieval、trace、admin 和 final answer 共用的基础审计单元，既保留请求参数，
 * 也保留工具返回的业务状态、错误语义和 snapshot miss 语义。</p>
 *
 * @param intentId 触发本次调用的意图标识
 * @param intentName 触发本次调用的意图名称
 * @param question 对应的子问题文本
 * @param toolId 实际调用的 MCP toolId
 * @param invocationMode 调用模式，例如本地、远端或未调用
 * @param requestParameters 发给 MCP 的结构化参数
 * @param success transport 级或调用层面的成功标记
 * @param status tool 级业务状态
 * @param message 工具返回的业务说明
 * @param error 工具返回的错误信息
 * @param snapshotMiss 是否命中图快照缺失
 * @param retryable 是否可重试
 * @param suggestedAction 建议动作，例如 RETRY_LATER
 * @param toolResult 原始 MCP 调用结果
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
     * 归一化 MCP 执行记录。
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

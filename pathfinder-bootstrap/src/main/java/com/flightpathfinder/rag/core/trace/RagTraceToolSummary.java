package com.flightpathfinder.rag.core.trace;

/**
 * 工具执行摘要（MCP）。
 *
 * @param toolId 工具标识
 * @param status 执行状态
 * @param message 摘要消息
 * @param snapshotMiss 是否命中快照缺失
 */
public record RagTraceToolSummary(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {

    /**
     * 构造时规整文本并补齐默认状态。
     */
    public RagTraceToolSummary {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
    }
}

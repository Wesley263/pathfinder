package com.flightpathfinder.rag.core.trace;

import java.time.Instant;

/**
 * 跟踪运行摘要。
 *
 * @param traceId 参数说明。
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param scene 场景
 * @param overallStatus 总体状态
 * @param snapshotMissOccurred 是否发生快照缺失
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param nodeCount 节点数
 * @param toolCount 工具记录数
 */
public record RagTraceRunSummary(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        Instant startedAt,
        Instant finishedAt,
        int nodeCount,
        int toolCount) {

    /**
     * 构造时规整文本并保护计数字段下界。
     */
    public RagTraceRunSummary {
        traceId = traceId == null ? "" : traceId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        scene = scene == null ? "" : scene.trim();
        overallStatus = overallStatus == null || overallStatus.isBlank() ? "UNKNOWN" : overallStatus.trim();
        nodeCount = Math.max(0, nodeCount);
        toolCount = Math.max(0, toolCount);
    }
}

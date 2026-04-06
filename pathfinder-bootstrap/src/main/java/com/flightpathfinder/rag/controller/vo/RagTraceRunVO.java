package com.flightpathfinder.rag.controller.vo;

import java.time.Instant;

/**
 * 追踪运行摘要展示对象。
 *
 * @param traceId trace 标识
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param scene 场景名
 * @param overallStatus trace 总状态
 * @param snapshotMissOccurred 是否发生过图快照缺失
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param nodeCount 节点数量
 * @param toolCount 工具调用数量
 */
public record RagTraceRunVO(
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
}


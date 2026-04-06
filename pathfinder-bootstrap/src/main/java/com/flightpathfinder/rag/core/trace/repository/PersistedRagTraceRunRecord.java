package com.flightpathfinder.rag.core.trace.repository;

import java.time.Instant;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
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
 * @param toolCount 工具数
 */
public record PersistedRagTraceRunRecord(
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



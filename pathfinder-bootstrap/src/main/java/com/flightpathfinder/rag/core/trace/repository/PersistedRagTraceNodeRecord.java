package com.flightpathfinder.rag.core.trace.repository;

import java.time.Instant;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param traceId 参数说明。
 * @param nodeIndex 节点序号
 * @param nodeName 节点名
 * @param nodeType 节点类型
 * @param status 节点状态
 * @param summary 节点摘要
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param attributesJson 参数说明。
 */
public record PersistedRagTraceNodeRecord(
        String traceId,
        int nodeIndex,
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        String attributesJson) {
}



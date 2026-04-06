package com.flightpathfinder.rag.core.trace.repository;

/**
 * 说明。
 *
 * @param traceId 参数说明。
 * @param toolIndex 工具序号
 * @param toolId 工具标识
 * @param status 工具状态
 * @param message 工具消息
 * @param snapshotMiss 是否快照缺失
 */
public record PersistedRagTraceToolRecord(
        String traceId,
        int toolIndex,
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {
}

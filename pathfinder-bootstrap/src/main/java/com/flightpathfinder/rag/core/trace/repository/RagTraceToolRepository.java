package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * Rag 工具调用摘要仓储抽象。
 *
 * 约束工具调用摘要的覆盖写入与按 traceId 查询能力。
 */
public interface RagTraceToolRepository {

    /**
     * 使用给定工具摘要替换指定 trace 的工具调用记录。
     *
     * @param traceId 链路追踪标识
     * @param records 完整有序工具摘要集合
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceToolRecord> records);

    /**
     * 查询指定 trace 的工具调用摘要。
     *
     * @param traceId 链路追踪标识
     * @return 按工具序号排序的工具摘要行
     */
    List<PersistedRagTraceToolRecord> findByTraceId(String traceId);
}

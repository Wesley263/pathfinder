package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * Rag 追踪节点仓储抽象。
 *
 * 约束节点明细的覆盖写入与按 traceId 查询能力。
 */
public interface RagTraceNodeRepository {

    /**
     * 使用给定节点记录集替换指定 trace 的节点数据。
     *
     * @param traceId 链路追踪标识
     * @param records 完整有序节点记录集
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceNodeRecord> records);

    /**
     * 查询指定 trace 的节点记录。
     *
     * @param traceId 链路追踪标识
     * @return 按节点序号排序的节点记录
     */
    List<PersistedRagTraceNodeRecord> findByTraceId(String traceId);
}

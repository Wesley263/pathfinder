package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * 持久化 trace 节点仓储接口。
 *
 * <p>节点与运行头分离存储，
 * 使阶段级与内部执行 breadcrumb 可独立查询，避免 run 表膨胀。
 */
public interface RagTraceNodeRepository {

    /**
     * 替换指定 traceId 的全部持久化节点。
     *
     * @param traceId 唯一 trace 标识
     * @param records 该 trace 的完整节点记录集
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceNodeRecord> records);

    /**
     * 加载单个 trace 的全部持久化节点。
     *
     * @param traceId 唯一 trace 标识
     * @return 按节点序号排序的节点记录
     */
    List<PersistedRagTraceNodeRecord> findByTraceId(String traceId);
}

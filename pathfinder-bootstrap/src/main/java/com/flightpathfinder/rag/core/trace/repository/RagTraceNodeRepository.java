package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface RagTraceNodeRepository {

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @param records 参数说明。
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceNodeRecord> records);

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @return 按节点序号排序的节点记录
     */
    List<PersistedRagTraceNodeRecord> findByTraceId(String traceId);
}

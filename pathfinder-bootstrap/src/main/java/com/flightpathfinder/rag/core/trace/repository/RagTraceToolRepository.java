package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface RagTraceToolRepository {

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @param records 参数说明。
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceToolRecord> records);

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @return 按工具序号排序的工具摘要行
     */
    List<PersistedRagTraceToolRecord> findByTraceId(String traceId);
}

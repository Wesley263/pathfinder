package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;
import java.util.Optional;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface RagTraceRunRepository {

    /**
     * 说明。
     *
     * @param record 单次请求对应的持久化运行记录
     */
    void upsert(PersistedRagTraceRunRecord record);

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @return 命中时返回运行记录
     */
    Optional<PersistedRagTraceRunRecord> findByTraceId(String traceId);

    /**
     * 按请求或会话标识过滤并列出近期运行记录。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 近期运行记录
     */
    List<PersistedRagTraceRunRecord> findRecent(String requestId, String conversationId, int limit);
}

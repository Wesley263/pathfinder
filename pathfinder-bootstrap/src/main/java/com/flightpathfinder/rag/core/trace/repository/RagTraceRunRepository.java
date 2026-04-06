package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;
import java.util.Optional;

/**
 * 持久化 trace 运行头仓储接口。
 *
 * <p>运行记录以 traceId 作为主键，
 * 并通过 requestId 与 conversationId 建立索引，承载请求级 trace 摘要。
 */
public interface RagTraceRunRepository {

    /**
     * 插入或更新单条 trace 运行头。
     *
     * @param record 单次请求对应的持久化运行记录
     */
    void upsert(PersistedRagTraceRunRecord record);

    /**
     * 按 traceId 查询单条运行记录。
     *
     * @param traceId 唯一 trace 标识
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

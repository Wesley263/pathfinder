package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;
import java.util.Optional;

/**
 * Rag 运行主记录仓储抽象。
 *
 * 提供运行记录写入、按 traceId 查询和近期列表查询能力。
 */
public interface RagTraceRunRepository {

    /**
        * 新增或更新一条运行主记录。
     *
     * @param record 单次请求对应的持久化运行记录
     */
    void upsert(PersistedRagTraceRunRecord record);

    /**
        * 根据 traceId 查询运行主记录。
     *
        * @param traceId 链路追踪标识
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

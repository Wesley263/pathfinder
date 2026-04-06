package com.flightpathfinder.rag.core.trace;

import java.util.List;
import java.util.Optional;

/**
 * Rag 追踪查询服务抽象。
 *
 * 对外提供运行详情与近期运行列表查询能力，
 * 使管理与审计视图可以独立于请求写路径演进。
 * 请求链路只需落事实，查询聚合由该服务承载。
 */
public interface RagTraceQueryService {

    /**
     * 按 traceId 查询完整追踪详情。
     *
     * @param traceId 链路追踪标识
     * @return 命中时返回追踪详情
     */
    Optional<RagTraceDetailResult> findDetail(String traceId);

    /**
     * 按过滤条件查询近期运行摘要列表。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 运行摘要列表
     */
    List<RagTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

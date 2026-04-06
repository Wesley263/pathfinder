package com.flightpathfinder.rag.core.trace;

import java.util.List;
import java.util.Optional;

/**
 * 说明。
 *
 * 说明。
 * 使管理与审计视图可以独立于请求写路径演进。
 * 请求链路只需落事实，查询聚合由该服务承载。
 */
public interface RagTraceQueryService {

    /**
     * 说明。
     *
     * @param traceId 参数说明。
     * @return 返回结果。
     */
    Optional<RagTraceDetailResult> findDetail(String traceId);

    /**
     * 说明。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 返回结果。
     */
    List<RagTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

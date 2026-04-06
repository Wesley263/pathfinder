package com.flightpathfinder.rag.core.trace;

import java.util.List;
import java.util.Optional;

/**
 * 面向读取侧的持久化 trace 查询服务。
 *
 * <p>查询职责与记录职责分离，
 * 使管理与审计视图可以独立于请求写路径演进。
 * 请求链路只需落事实，查询聚合由该服务承载。
 */
public interface RagTraceQueryService {

    /**
     * 加载单条持久化 trace 详情视图。
     *
     * @param traceId 单次请求生成的唯一 trace 标识
     * @return 命中时返回 trace 详情
     */
    Optional<RagTraceDetailResult> findDetail(String traceId);

    /**
     * 按请求标识或会话标识过滤并列出近期 trace 运行记录。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 近期 trace 运行摘要列表
     */
    List<RagTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

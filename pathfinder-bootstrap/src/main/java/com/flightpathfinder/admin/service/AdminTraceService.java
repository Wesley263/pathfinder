package com.flightpathfinder.admin.service;

import java.util.List;
import java.util.Optional;

/**
 * 持久化追踪的管理端查询服务。
 *
 * <p>该服务以管理端视角组织追踪数据，
 * 避免管理 API 复用或泄露 RAG 核心内部追踪查询模型。
 */
public interface AdminTraceService {

    /**
        * 查询单个追踪的管理端详情视图。
     *
     * @param traceId unique trace identifier
     * @return admin trace detail when found
     */
    Optional<AdminTraceDetailResult> findDetail(String traceId);

    /**
        * 按请求或会话标识过滤并列出最近追踪。
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of runs to return
     * @return admin-facing trace summaries
     */
    List<AdminTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

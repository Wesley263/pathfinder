package com.flightpathfinder.admin.service;

import java.util.List;
import java.util.Optional;

/**
 * 持久化追踪的管理端查询服务。
 *
 * 面向管理端提供追踪详情与运行列表查询能力。
 */
public interface AdminTraceService {

    /**
        * 查询单个追踪的管理端详情视图。
     *
     * @param traceId 追踪标识
     * @return 追踪详情
     */
    Optional<AdminTraceDetailResult> findDetail(String traceId);

    /**
        * 按请求或会话标识过滤并列出最近追踪。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 最近追踪列表
     */
    List<AdminTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}


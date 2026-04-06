package com.flightpathfinder.admin.service;

import java.util.List;
import java.util.Optional;

/**
 * 持久化追踪的管理端查询服务。
 *
 * 说明。
 * 说明。
 */
public interface AdminTraceService {

    /**
        * 查询单个追踪的管理端详情视图。
     *
     * @param traceId 参数说明。
     * @return 返回结果。
     */
    Optional<AdminTraceDetailResult> findDetail(String traceId);

    /**
        * 按请求或会话标识过滤并列出最近追踪。
     *
     * @param requestId 参数说明。
     * @param conversationId 参数说明。
     * @param limit 参数说明。
     * @return 返回结果。
     */
    List<AdminTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

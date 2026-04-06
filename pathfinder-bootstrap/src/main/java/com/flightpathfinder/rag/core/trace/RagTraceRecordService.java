package com.flightpathfinder.rag.core.trace;

/**
 * 面向持久化层的最终 trace 写服务。
 *
 * <p>写入关注点与查询关注点分离，
 * 使请求链路只负责记录 trace 事实，不承担管理侧读模型与列表查询职责。
 */
public interface RagTraceRecordService {

    /**
     * 持久化单次请求的最终 trace 结果。
     *
     * @param traceResult 已完成收尾的 trace 结果
     */
    void persist(RagTraceResult traceResult);
}

package com.flightpathfinder.rag.core.trace;

/**
 * Rag 追踪持久化服务抽象。
 *
 * 统一定义追踪结果写入入口。
 */
public interface RagTraceRecordService {

    /**
     * 持久化一次追踪结果。
     *
     * @param traceResult 请求追踪结果
     */
    void persist(RagTraceResult traceResult);
}


package com.flightpathfinder.rag.core.pipeline;

/**
 * Rag 阶段一编排接口。
 *
 * 负责改写、意图解析和路由相关的前置决策流程。
 */
public interface StageOneRagPipeline {

    /**
     * 执行阶段一编排。
     *
     * @param request 原始问题及本次请求上下文，包含会话记忆等只读输入
     * @return 阶段一产物（改写结果与路由结果）
     */
    StageOneRagResult run(StageOneRagRequest request);
}



package com.flightpathfinder.rag.core.pipeline;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface StageOneRagPipeline {

    /**
     * 说明。
     *
     * @param request 原始问题及本次请求上下文，包含会话记忆等只读输入
     * @return 返回结果。
     */
    StageOneRagResult run(StageOneRagRequest request);
}


package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段的编排边界。
 *
 * 说明。
 * 说明。
 */
public interface RetrievalService {

    /**
     * 说明。
     *
     * @param stageOneRagResult 第一阶段输出，包含改写结果与分流结果
     * @return 返回结果。
     */
    RetrievalResult retrieve(StageOneRagResult stageOneRagResult);
}


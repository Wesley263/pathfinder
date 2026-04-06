package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段的编排边界。
 *
 * 接收第一阶段输出，执行 KB 与 MCP 分支并生成统一检索结果。
 */
public interface RetrievalService {

    /**
     * 执行检索阶段。
     *
     * @param stageOneRagResult 第一阶段输出，包含改写结果与分流结果
     * @return 检索阶段总结果
     */
    RetrievalResult retrieve(StageOneRagResult stageOneRagResult);
}


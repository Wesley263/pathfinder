package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * 检索阶段的编排边界。
 *
 * <p>这一层接收已经完成分流的 stage one 结果，分别触发 KB 检索与 MCP 执行，
 * 再把两路结果统一交给 final answer 阶段。</p>
 */
public interface RetrievalService {

    /**
     * 执行 retrieval 阶段。
     *
     * @param stageOneRagResult 第一阶段输出，包含改写结果与分流结果
     * @return 聚合后的 retrieval 结果，内部同时保留 KB 和 MCP 上下文
     */
    RetrievalResult retrieve(StageOneRagResult stageOneRagResult);
}


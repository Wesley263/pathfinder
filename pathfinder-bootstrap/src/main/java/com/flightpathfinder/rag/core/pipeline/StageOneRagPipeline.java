package com.flightpathfinder.rag.core.pipeline;

/**
 * 面向 RAG 第一阶段的编排边界。
 *
 * <p>这一阶段故意只做到问题改写、意图分类和 KB/MCP 分流准备，不把 retrieval 和
 * 将最终回答提前纳入，目的是让主链每一段输入输出都保持可审计、可替换。</p>
 */
public interface StageOneRagPipeline {

    /**
     * 执行 RAG 第一阶段。
     *
     * @param request 原始问题及本次请求上下文，包含会话记忆等只读输入
     * @return 第一阶段产物，包含改写结果、意图解析结果和 KB/MCP 分流结果
     */
    StageOneRagResult run(StageOneRagRequest request);
}


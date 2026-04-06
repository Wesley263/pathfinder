package com.flightpathfinder.rag.core.rewrite;

import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;

/**
 * 第一阶段中的问题改写边界。
 *
 * <p>这一层只做输入整理：规范术语、拆子问题、必要时结合 memory 补全追问上下文，
 * 但不直接对问题做路由裁决，路由决策仍留给 intent 层。</p>
 */
public interface QuestionRewriteService {

    /**
     * 把原始问题改写成适合后续路由与展示的统一表示。
     *
     * @param request 原始问题及请求级上下文
     * @return 改写结果，包含展示用问题、子问题以及内部路由用问题视图
     */
    RewriteResult rewrite(StageOneRagRequest request);
}

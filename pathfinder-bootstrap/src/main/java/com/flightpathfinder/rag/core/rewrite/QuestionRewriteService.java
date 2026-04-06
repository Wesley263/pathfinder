package com.flightpathfinder.rag.core.rewrite;

import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;

/**
 * 第一阶段中的问题改写边界。
 *
 * 说明。
 * 说明。
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

package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentResolver;
import com.flightpathfinder.rag.core.rewrite.QuestionRewriteService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import org.springframework.stereotype.Service;

/**
 * 第一阶段主链的默认实现。
 *
 * 串联问题改写与意图解析，输出后续检索可直接消费的分流结果。
 */
@Service
public class DefaultStageOneRagPipeline implements StageOneRagPipeline {

    /** 承担问题改写职责的服务。 */
    private final QuestionRewriteService questionRewriteService;
    /** 承担意图分类与分流汇总职责的服务。 */
    private final IntentResolver intentResolver;

    /**
     * 构造第一阶段默认实现。
     *
     * @param questionRewriteService 问题改写服务
     * @param intentResolver 意图解析与分流服务
     */
    public DefaultStageOneRagPipeline(QuestionRewriteService questionRewriteService, IntentResolver intentResolver) {
        this.questionRewriteService = questionRewriteService;
        this.intentResolver = intentResolver;
    }

    /**
     * 先做问题改写，再做意图解析。
     *
     * @param request 原始问题及本次请求上下文
     * @return 阶段一执行结果
     */
    @Override
    public StageOneRagResult run(StageOneRagRequest request) {
        // 先改写问题，保证意图解析面对的是规整后的表达。
        RewriteResult rewriteResult = questionRewriteService.rewrite(request);

        // 意图解析独立成第二步，这样第一阶段既能保留打分明细，又能产出面向后续阶段的统一分流结果。
        IntentResolution intentResolution = intentResolver.resolve(rewriteResult);
        return new StageOneRagResult(rewriteResult, intentResolution, intentResolution.splitResult(),
                request == null ? null : request.memoryContext());
    }
}


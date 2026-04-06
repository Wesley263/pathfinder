package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentResolver;
import com.flightpathfinder.rag.core.rewrite.QuestionRewriteService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import org.springframework.stereotype.Service;

/**
 * 第一阶段主链的默认实现。
 *
 * <p>它位于 pipeline 层，是因为这里负责把 rewrite 与 intent 两个子能力串起来；
 * 但它又刻意停在 retrieval 之前，避免第一阶段膨胀成“全链路黑盒”，方便后续对每段结果做审计。</p>
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
     * @return 可直接交给 retrieval 阶段的第一阶段结果
     */
    @Override
    public StageOneRagResult run(StageOneRagRequest request) {
        // 改写必须先发生，因为术语归一和追问补全会直接影响后续到底该走 KB、MCP 还是 SYSTEM。
        RewriteResult rewriteResult = questionRewriteService.rewrite(request);

        // 意图解析独立成第二步，这样第一阶段既能保留打分明细，又能产出面向后续阶段的统一分流结果。
        IntentResolution intentResolution = intentResolver.resolve(rewriteResult);
        return new StageOneRagResult(rewriteResult, intentResolution, intentResolution.splitResult(),
                request == null ? null : request.memoryContext());
    }
}

package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentResolver;
import com.flightpathfinder.rag.core.rewrite.QuestionRewriteService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the first RAG stage.
 *
 * <p>This class lives in the pipeline layer because it coordinates rewrite and intent
 * services, but deliberately stops before retrieval so stage one can stay reusable and easy
 * to inspect in isolation.</p>
 */
@Service
public class DefaultStageOneRagPipeline implements StageOneRagPipeline {

    private final QuestionRewriteService questionRewriteService;
    private final IntentResolver intentResolver;

    public DefaultStageOneRagPipeline(QuestionRewriteService questionRewriteService, IntentResolver intentResolver) {
        this.questionRewriteService = questionRewriteService;
        this.intentResolver = intentResolver;
    }

    /**
     * Rewrites first and resolves intents second so downstream stages operate on normalized,
     * split-aware routing input instead of raw user wording.
     *
     * @param request original question plus request-scoped context
     * @return stage-one result consumed by retrieval
     */
    @Override
    public StageOneRagResult run(StageOneRagRequest request) {
        // Rewrite comes first because terminology normalization and follow-up expansion can
        // materially change which branch should receive the question.
        RewriteResult rewriteResult = questionRewriteService.rewrite(request);

        // Intent resolution remains a separate step so stage one can expose both the scored
        // intent view and the merged KB/MCP split as an auditable hand-off.
        IntentResolution intentResolution = intentResolver.resolve(rewriteResult);
        return new StageOneRagResult(rewriteResult, intentResolution, intentResolution.splitResult(),
                request == null ? null : request.memoryContext());
    }
}

package com.flightpathfinder.rag.core.rewrite;

import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;

/**
 * Rewrite boundary for the first RAG stage.
 *
 * <p>The rewrite layer normalizes wording and optionally expands follow-up questions with
 * memory context, but it does not decide routing on its own.</p>
 */
public interface QuestionRewriteService {

    /**
     * Rewrites the incoming question into a routing-friendly representation.
     *
     * @param request original question plus request-scoped context
     * @return normalized question, sub-question view, and internal routing variants
     */
    RewriteResult rewrite(StageOneRagRequest request);
}

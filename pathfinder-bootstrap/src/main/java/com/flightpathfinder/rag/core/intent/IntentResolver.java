package com.flightpathfinder.rag.core.intent;

import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * Resolves rewritten questions into merged routing decisions.
 *
 * <p>The resolver sits above the classifier because it sees all rewritten sub-questions and
 * owns the final KB/MCP/SYSTEM split semantics for downstream retrieval.</p>
 */
public interface IntentResolver {

    /**
     * Resolves rewrite output into scored sub-question intents and merged split results.
     *
     * @param rewriteResult rewrite output from stage one
     * @return merged intent resolution used by retrieval
     */
    IntentResolution resolve(RewriteResult rewriteResult);
}

package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * KB-side retrieval boundary.
 *
 * <p>This interface stays unaware of MCP execution so knowledge retrieval can evolve
 * independently from tool-calling concerns.</p>
 */
public interface KnowledgeRetriever {

    /**
     * Retrieves KB context for the current rewritten question and KB intents.
     *
     * @param rewriteResult rewritten question used for retrieval
     * @param intentSplitResult split result containing KB intents
     * @return structured KB context, including empty and error semantics
     */
    KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

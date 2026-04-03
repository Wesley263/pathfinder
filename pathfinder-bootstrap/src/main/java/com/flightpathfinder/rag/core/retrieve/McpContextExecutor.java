package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * MCP-side execution boundary for the retrieval stage.
 *
 * <p>This interface converts split MCP intents into executable tool calls and returns a
 * structured MCP context without taking over final answer generation.</p>
 */
public interface McpContextExecutor {

    /**
     * Executes MCP intents for the current rewritten question.
     *
     * @param rewriteResult rewritten question used for parameter extraction
     * @param intentSplitResult split result containing MCP intents
     * @return structured MCP context, including partial, miss, and error semantics
     */
    McpContext execute(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

public record StageOneRagResult(
        RewriteResult rewriteResult,
        IntentResolution intentResolution,
        IntentSplitResult intentSplitResult,
        ConversationMemoryContext memoryContext) {

    public StageOneRagResult {
        rewriteResult = rewriteResult == null ? new RewriteResult("", null, null, null) : rewriteResult;
        intentResolution = intentResolution == null ? IntentResolution.empty() : intentResolution;
        intentSplitResult = intentSplitResult == null ? intentResolution.splitResult() : intentSplitResult;
        memoryContext = memoryContext == null ? ConversationMemoryContext.empty("") : memoryContext;
    }
}

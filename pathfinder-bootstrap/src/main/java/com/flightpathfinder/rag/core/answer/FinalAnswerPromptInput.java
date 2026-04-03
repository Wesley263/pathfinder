package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import java.util.List;

public record FinalAnswerPromptInput(
        String rewrittenQuestion,
        IntentSplitResult intentSplitResult,
        KbContext kbContext,
        McpContext mcpContext,
        ConversationMemoryContext memoryContext,
        List<AnswerEvidenceSummary> evidenceSummaries,
        boolean partial,
        boolean snapshotMissAffected,
        boolean empty) {

    public FinalAnswerPromptInput {
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        intentSplitResult = intentSplitResult == null ? IntentSplitResult.empty() : intentSplitResult;
        kbContext = kbContext == null
                ? KbContext.empty("no KB context", List.of())
                : kbContext;
        mcpContext = mcpContext == null
                ? McpContext.skipped("no MCP context")
                : mcpContext;
        memoryContext = memoryContext == null
                ? ConversationMemoryContext.empty("")
                : memoryContext;
        evidenceSummaries = List.copyOf(evidenceSummaries == null ? List.of() : evidenceSummaries);
    }
}

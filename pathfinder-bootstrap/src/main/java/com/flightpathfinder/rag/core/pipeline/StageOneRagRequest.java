package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;

public record StageOneRagRequest(
        String originalQuestion,
        String conversationId,
        String requestId,
        ConversationMemoryContext memoryContext) {

    public StageOneRagRequest {
        originalQuestion = originalQuestion == null ? "" : originalQuestion.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        memoryContext = memoryContext == null
                ? ConversationMemoryContext.empty(conversationId)
                : memoryContext;
    }
}

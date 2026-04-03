package com.flightpathfinder.rag.core.memory.repository;

import java.time.Instant;

public record ConversationMemoryMessageRecord(
        String messageId,
        String conversationId,
        String requestId,
        int messageIndex,
        String role,
        String content,
        String rewrittenQuestion,
        String answerStatus,
        Instant createdAt) {

    public ConversationMemoryMessageRecord {
        messageId = messageId == null ? "" : messageId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        role = role == null ? "" : role.trim();
        content = content == null ? "" : content.trim();
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        answerStatus = answerStatus == null ? "" : answerStatus.trim();
        messageIndex = Math.max(0, messageIndex);
    }
}

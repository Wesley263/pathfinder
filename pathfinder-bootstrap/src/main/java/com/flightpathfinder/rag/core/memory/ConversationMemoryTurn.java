package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

public record ConversationMemoryTurn(
        String requestId,
        String userQuestion,
        String rewrittenQuestion,
        String answerText,
        String answerStatus,
        Instant createdAt) {

    public ConversationMemoryTurn {
        requestId = requestId == null ? "" : requestId.trim();
        userQuestion = userQuestion == null ? "" : userQuestion.trim();
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        answerText = answerText == null ? "" : answerText.trim();
        answerStatus = answerStatus == null ? "" : answerStatus.trim();
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String questionForContext() {
        return rewrittenQuestion.isBlank() ? userQuestion : rewrittenQuestion;
    }
}

package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

public record AdminConversationMessageVO(
        String messageId,
        String requestId,
        int messageIndex,
        String role,
        String content,
        String rewrittenQuestion,
        String answerStatus,
        Instant createdAt) {
}

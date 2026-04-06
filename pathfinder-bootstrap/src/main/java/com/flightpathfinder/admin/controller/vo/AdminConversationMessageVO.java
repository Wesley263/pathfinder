package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
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


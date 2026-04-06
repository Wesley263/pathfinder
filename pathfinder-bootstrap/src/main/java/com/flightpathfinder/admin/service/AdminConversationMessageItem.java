package com.flightpathfinder.admin.service;

import java.time.Instant;

/**
 * 管理端服务层数据模型。
 */
public record AdminConversationMessageItem(
        String messageId,
        String requestId,
        int messageIndex,
        String role,
        String content,
        String rewrittenQuestion,
        String answerStatus,
        Instant createdAt) {

    public AdminConversationMessageItem {
        messageId = messageId == null ? "" : messageId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        role = role == null ? "" : role.trim();
        content = content == null ? "" : content.trim();
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        answerStatus = answerStatus == null ? "" : answerStatus.trim();
        messageIndex = Math.max(0, messageIndex);
    }
}


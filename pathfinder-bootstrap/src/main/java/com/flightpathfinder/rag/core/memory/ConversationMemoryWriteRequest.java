package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

/**
 * 会话记忆写入请求。
 *
 * @param conversationId 会话标识
 * @param requestId 请求标识
 * @param userQuestion 用户原始问题
 * @param rewrittenQuestion 改写后问题
 * @param answerText 回答文本
 * @param answerStatus 回答状态
 * @param createdAt 写入时间
 */
public record ConversationMemoryWriteRequest(
        String conversationId,
        String requestId,
        String userQuestion,
        String rewrittenQuestion,
        String answerText,
        String answerStatus,
        Instant createdAt) {

    /**
     * 构造时规整字符串字段并补齐默认时间。
     */
    public ConversationMemoryWriteRequest {
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        userQuestion = userQuestion == null ? "" : userQuestion.trim();
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        answerText = answerText == null ? "" : answerText.trim();
        answerStatus = answerStatus == null ? "" : answerStatus.trim();
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}

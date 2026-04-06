package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

/**
 * 会话中的单轮用户/助手交互。
 *
 * @param requestId 请求标识
 * @param userQuestion 用户原始问题
 * @param rewrittenQuestion 改写后问题
 * @param answerText 助手回答文本
 * @param answerStatus 回答状态
 * @param createdAt 轮次创建时间
 */
public record ConversationMemoryTurn(
        String requestId,
        String userQuestion,
        String rewrittenQuestion,
        String answerText,
        String answerStatus,
        Instant createdAt) {

    /**
     * 构造时规整文本字段并补齐默认时间。
     */
    public ConversationMemoryTurn {
        requestId = requestId == null ? "" : requestId.trim();
        userQuestion = userQuestion == null ? "" : userQuestion.trim();
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        answerText = answerText == null ? "" : answerText.trim();
        answerStatus = answerStatus == null ? "" : answerStatus.trim();
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * 返回用于上下文拼接的问题文本，优先改写问题。
     *
     * @return 改写问题优先，否则返回原问题
     */
    public String questionForContext() {
        return rewrittenQuestion.isBlank() ? userQuestion : rewrittenQuestion;
    }
}

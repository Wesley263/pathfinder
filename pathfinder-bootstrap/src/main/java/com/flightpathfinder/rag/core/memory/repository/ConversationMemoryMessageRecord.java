package com.flightpathfinder.rag.core.memory.repository;

import java.time.Instant;

/**
 * 会话消息持久化记录行。
 *
 * @param messageId 消息主键
 * @param conversationId 会话标识
 * @param requestId 请求标识
 * @param messageIndex 同一请求内消息序号
 * @param role 参数说明。
 * @param content 消息正文
 * @param rewrittenQuestion 改写问题文本
 * @param answerStatus 回答状态
 * @param createdAt 创建时间
 */
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

    /**
     * 构造时规整字符串字段并保护消息序号下界。
     */
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

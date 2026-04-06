package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

/**
 * 会话级记忆元信息。
 *
 * @param conversationId 会话标识
 * @param lastRequestId 最近一次请求标识
 * @param turnCount 累计轮次数
 * @param messageCount 累计消息条数
 * @param createdAt 首次创建时间
 * @param updatedAt 最近更新时间
 */
public record ConversationMemoryConversation(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 构造时做字符串规整与计数下界保护。
     */
    public ConversationMemoryConversation {
        conversationId = conversationId == null ? "" : conversationId.trim();
        lastRequestId = lastRequestId == null ? "" : lastRequestId.trim();
        turnCount = Math.max(0, turnCount);
        messageCount = Math.max(0, messageCount);
    }

    /**
     * 创建指定会话标识的空元信息对象。
     *
     * @param conversationId 会话标识
     * @return 空会话元信息
     */
    public static ConversationMemoryConversation empty(String conversationId) {
        return new ConversationMemoryConversation(conversationId, "", 0, 0, null, null);
    }
}

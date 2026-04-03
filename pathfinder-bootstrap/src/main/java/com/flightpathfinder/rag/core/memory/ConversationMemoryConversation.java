package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

public record ConversationMemoryConversation(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        Instant createdAt,
        Instant updatedAt) {

    public ConversationMemoryConversation {
        conversationId = conversationId == null ? "" : conversationId.trim();
        lastRequestId = lastRequestId == null ? "" : lastRequestId.trim();
        turnCount = Math.max(0, turnCount);
        messageCount = Math.max(0, messageCount);
    }

    public static ConversationMemoryConversation empty(String conversationId) {
        return new ConversationMemoryConversation(conversationId, "", 0, 0, null, null);
    }
}

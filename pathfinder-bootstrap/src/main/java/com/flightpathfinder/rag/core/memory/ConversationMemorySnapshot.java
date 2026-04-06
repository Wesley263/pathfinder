package com.flightpathfinder.rag.core.memory;

import java.util.List;

/**
 * 从记忆存储中读取到的原始近期轮次快照。
 *
 * <p>该快照有意保持“存储形态”：先携带会话元信息与显式近期轮次，
 * 再由上层在 {@link ConversationMemoryContext} 中合并摘要层信息。
 */
public record ConversationMemorySnapshot(
        ConversationMemoryConversation conversation,
        List<ConversationMemoryTurn> turns) {

    public ConversationMemorySnapshot {
        conversation = conversation == null
                ? ConversationMemoryConversation.empty("")
                : conversation;
        turns = List.copyOf(turns == null ? List.of() : turns);
    }

    /**
     * 为指定会话标识创建空存储快照。
     *
     * @param conversationId 空结果关联的会话标识
     * @return 空快照
     */
    public static ConversationMemorySnapshot empty(String conversationId) {
        return new ConversationMemorySnapshot(ConversationMemoryConversation.empty(conversationId), List.of());
    }
}

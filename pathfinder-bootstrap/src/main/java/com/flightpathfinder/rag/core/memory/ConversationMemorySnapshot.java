package com.flightpathfinder.rag.core.memory;

import java.util.List;

/**
 * Raw recent-turn snapshot loaded from the memory store.
 *
 * <p>This snapshot is intentionally store-shaped: it carries conversation metadata plus explicit recent turns
 * before any summary layer is merged into the higher-level {@link ConversationMemoryContext}.
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
     * Creates an empty store snapshot for the given conversation id.
     *
     * @param conversationId conversation identifier associated with the empty result
     * @return empty snapshot
     */
    public static ConversationMemorySnapshot empty(String conversationId) {
        return new ConversationMemorySnapshot(ConversationMemoryConversation.empty(conversationId), List.of());
    }
}

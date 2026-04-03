package com.flightpathfinder.rag.core.memory;

/**
 * Persistence-facing abstraction for recent-turn memory data.
 *
 * <p>The store owns conversation and message persistence, while summary generation remains a separate concern.
 * This split keeps raw history retrieval simple and lets summary policies evolve without changing write-path
 * storage contracts.
 */
public interface ConversationMemoryStore {

    /**
     * Loads the latest recent turns for the given conversation.
     *
     * @param conversationId stable conversation identifier
     * @param recentTurnLimit number of recent turns requested by the caller
     * @return conversation snapshot containing metadata and recent turns, or an empty snapshot when no history
     *     exists
     */
    ConversationMemorySnapshot load(String conversationId, int recentTurnLimit);

    /**
     * Appends one completed user/assistant turn to storage.
     *
     * @param conversationId stable conversation identifier
     * @param turn normalized turn assembled after final answer generation
     */
    void appendTurn(String conversationId, ConversationMemoryTurn turn);
}

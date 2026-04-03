package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryTurn;
import java.util.List;

/**
 * Repository for raw conversation message rows.
 *
 * <p>Messages are stored row-by-row instead of as already-assembled turns so user and assistant content can
 * be audited independently and later reassembled into different higher-level projections.
 */
public interface ConversationMemoryMessageRepository {

    /**
     * Persists one completed conversation turn as explicit USER and ASSISTANT message rows.
     *
     * @param conversationId stable conversation identifier
     * @param turn completed turn to persist
     */
    void saveTurn(String conversationId, ConversationMemoryTurn turn);

    /**
     * Loads the latest message rows for a conversation.
     *
     * @param conversationId stable conversation identifier
     * @param limit maximum number of message rows to return
     * @return recent message rows ordered for turn reassembly
     */
    List<ConversationMemoryMessageRecord> findRecentByConversationId(String conversationId, int limit);

    /**
     * Loads all message rows for a conversation.
     *
     * @param conversationId stable conversation identifier
     * @return all message rows ordered oldest to newest
     */
    List<ConversationMemoryMessageRecord> findAllByConversationId(String conversationId);
}

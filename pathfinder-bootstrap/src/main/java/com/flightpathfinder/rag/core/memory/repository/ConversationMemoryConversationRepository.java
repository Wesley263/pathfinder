package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryConversation;
import java.util.List;
import java.util.Optional;

/**
 * Repository for conversation-level memory metadata.
 *
 * <p>This repository only manages conversation headers and counters. Individual messages and summaries live in
 * separate repositories so each layer can evolve independently.
 */
public interface ConversationMemoryConversationRepository {

    /**
     * Finds one conversation header by id.
     *
     * @param conversationId stable conversation identifier
     * @return conversation metadata when present
     */
    Optional<ConversationMemoryConversation> findByConversationId(String conversationId);

    /**
     * Lists recent conversations for admin and lookup use cases.
     *
     * @param conversationId optional exact filter; blank means "list recent conversations"
     * @param limit maximum number of rows to return
     * @return recent conversation headers sorted by update time
     */
    List<ConversationMemoryConversation> findRecent(String conversationId, int limit);

    /**
     * Upserts the conversation header after a completed turn is written.
     *
     * @param conversationId stable conversation identifier
     * @param requestId request id associated with the latest completed turn
     */
    void upsertAfterTurn(String conversationId, String requestId);
}

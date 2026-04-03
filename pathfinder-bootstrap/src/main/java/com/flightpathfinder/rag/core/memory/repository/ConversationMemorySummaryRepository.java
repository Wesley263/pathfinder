package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import java.util.Optional;

/**
 * Repository for persisted conversation summaries.
 *
 * <p>Summary storage is separate from raw messages so summary text can be regenerated or replaced without
 * mutating the underlying conversation history.
 */
public interface ConversationMemorySummaryRepository {

    /**
     * Finds the latest summary for the given conversation.
     *
     * @param conversationId stable conversation identifier
     * @return persisted summary when present
     */
    Optional<ConversationMemorySummary> findByConversationId(String conversationId);

    /**
     * Inserts or updates the summary text for the given conversation.
     *
     * @param conversationId stable conversation identifier
     * @param summaryText compacted summary text
     * @param summarizedTurnCount number of historical turns already represented by the summary
     */
    void upsert(String conversationId, String summaryText, int summarizedTurnCount);
}

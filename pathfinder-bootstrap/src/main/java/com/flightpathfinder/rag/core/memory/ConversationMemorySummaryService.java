package com.flightpathfinder.rag.core.memory;

/**
 * Summary-management abstraction for conversation memory.
 *
 * <p>Summary is modeled separately from raw message storage because compaction rules are policy-driven and
 * optional. The mainline can therefore use recent turns, summary, or both without coupling write storage to
 * summarization strategy.
 */
public interface ConversationMemorySummaryService {

    /**
     * Loads the latest summary for the conversation if summary support is enabled.
     *
     * @param conversationId stable conversation identifier
     * @return persisted summary or an empty summary object when unavailable
     */
    ConversationMemorySummary loadLatestSummary(String conversationId);

    /**
     * Refreshes summary state when the conversation has grown past the configured threshold.
     *
     * @param conversationId stable conversation identifier
     */
    void refreshIfNeeded(String conversationId);
}

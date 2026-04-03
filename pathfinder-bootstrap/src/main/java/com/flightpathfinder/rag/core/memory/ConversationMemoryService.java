package com.flightpathfinder.rag.core.memory;

/**
 * Application-facing entry point for conversation memory.
 *
 * <p>This service sits above store and summary components so the RAG orchestrator can load and write
 * conversation context without knowing whether memory is backed by JDBC, in-memory fallbacks, or summary
 * compaction policies.
 */
public interface ConversationMemoryService {

    /**
     * Loads the memory context for the given conversation.
     *
     * @param conversationId stable conversation identifier carried by the user-facing API; blank means
     *     "run without memory"
     * @return memory context containing recent turns and optional summary; empty context when the id is blank
     *     or no history exists
     */
    ConversationMemoryContext loadContext(String conversationId);

    /**
     * Appends the current user/assistant turn into memory storage.
     *
     * @param writeRequest normalized write request for the completed turn
     */
    void appendTurn(ConversationMemoryWriteRequest writeRequest);
}

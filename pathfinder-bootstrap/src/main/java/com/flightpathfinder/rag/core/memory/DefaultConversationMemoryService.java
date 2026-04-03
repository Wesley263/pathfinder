package com.flightpathfinder.rag.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default orchestrator for memory load and write operations.
 *
 * <p>This layer keeps memory lifecycle concerns out of stage-one, retrieval and answer services. It combines
 * recent-turn storage with optional summary loading so the mainline can consume a single memory context.
 */
@Service
public class DefaultConversationMemoryService implements ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultConversationMemoryService.class);

    private final ConversationMemoryStore conversationMemoryStore;
    private final ConversationMemorySummaryService conversationMemorySummaryService;
    private final ConversationMemoryProperties conversationMemoryProperties;

    public DefaultConversationMemoryService(ConversationMemoryStore conversationMemoryStore,
                                            ConversationMemorySummaryService conversationMemorySummaryService,
                                            ConversationMemoryProperties conversationMemoryProperties) {
        this.conversationMemoryStore = conversationMemoryStore;
        this.conversationMemorySummaryService = conversationMemorySummaryService;
        this.conversationMemoryProperties = conversationMemoryProperties;
    }

    /**
     * Loads the current memory context for the requested conversation id.
     *
     * @param conversationId conversation identifier from the current query; blank means the request should not
     *     use memory
     * @return empty context when the id is blank or no history exists, otherwise recent turns plus optional
     *     summary
     */
    @Override
    public ConversationMemoryContext loadContext(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return ConversationMemoryContext.empty("");
        }
        ConversationMemorySnapshot snapshot = conversationMemoryStore.load(
                conversationId,
                conversationMemoryProperties.recentTurnLimit());
        ConversationMemorySummary summary;
        try {
            summary = conversationMemorySummaryService.loadLatestSummary(conversationId);
        } catch (RuntimeException exception) {
            log.warn("Failed to load conversation summary for conversationId={}", conversationId, exception);
            summary = ConversationMemorySummary.empty();
        }
        // Recent turns and summary are loaded independently so summary failures degrade gracefully instead of
        // blocking the whole query path.
        if (snapshot.turns().isEmpty() && !summary.exists()) {
            return ConversationMemoryContext.empty(conversationId);
        }
        return new ConversationMemoryContext(snapshot.conversation(), summary, snapshot.turns());
    }

    /**
     * Persists the completed turn and refreshes summary state when needed.
     *
     * @param writeRequest completed-turn payload derived from the current query lifecycle
     */
    @Override
    public void appendTurn(ConversationMemoryWriteRequest writeRequest) {
        if (writeRequest == null || writeRequest.conversationId().isBlank()) {
            return;
        }
        conversationMemoryStore.appendTurn(
                writeRequest.conversationId(),
                new ConversationMemoryTurn(
                        writeRequest.requestId(),
                        writeRequest.userQuestion(),
                        writeRequest.rewrittenQuestion(),
                        writeRequest.answerText(),
                        writeRequest.answerStatus(),
                        writeRequest.createdAt()));
        try {
            // Summary refresh is threshold-driven and best-effort: failed compaction must not block the main
            // write path for the just-finished turn.
            conversationMemorySummaryService.refreshIfNeeded(writeRequest.conversationId());
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh conversation summary for conversationId={}", writeRequest.conversationId(), exception);
        }
    }
}

package com.flightpathfinder.rag.core.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Memory context consumed by the RAG mainline.
 *
 * <p>The context intentionally carries both recent turns and optional summary. Recent turns preserve the most
 * recent wording exactly, while summary gives stage one and retrieval enough background once conversations
 * grow beyond the recent-turn window.
 */
public record ConversationMemoryContext(
        ConversationMemoryConversation conversation,
        ConversationMemorySummary summary,
        List<ConversationMemoryTurn> recentTurns) {

    public ConversationMemoryContext {
        conversation = conversation == null
                ? ConversationMemoryConversation.empty("")
                : conversation;
        summary = summary == null ? ConversationMemorySummary.empty() : summary;
        recentTurns = List.copyOf(recentTurns == null ? List.of() : recentTurns);
    }

    /**
     * Indicates whether the context contains any usable memory.
     *
     * @return {@code true} when both recent turns and summary are absent
     */
    public boolean empty() {
        return recentTurns.isEmpty() && !summary.exists();
    }

    /**
     * Returns the conversation id associated with this memory context.
     *
     * @return conversation id, possibly blank when the current request does not use memory
     */
    public String conversationId() {
        return conversation.conversationId();
    }

    /**
     * Returns the total persisted turn count known for the conversation.
     *
     * @return total turn count from conversation metadata
     */
    public int totalTurnCount() {
        return conversation.turnCount();
    }

    /**
     * Indicates whether a summary fragment is available.
     *
     * @return {@code true} when summary text exists for older turns
     */
    public boolean hasSummary() {
        return summary.exists();
    }

    /**
     * Flattens summary and recent turns into the compact text fragment used by the current mainline.
     *
     * @return routing-friendly history text; empty string when there is no memory to inject
     */
    public String routingHistoryText() {
        List<String> fragments = new ArrayList<>();
        if (summary.exists()) {
            fragments.add("Summary: " + summary.summaryText());
        }
        // Summary covers older turns, while recent turns preserve the exact latest phrasing. Both are included
        // so rewrite and routing do not lose either long-horizon context or immediate conversational nuance.
        recentTurns.stream()
                .map(turn -> "User: " + turn.questionForContext() + " | Assistant: " + abbreviate(turn.answerText()))
                .forEach(fragments::add);
        if (fragments.isEmpty()) {
            return "";
        }
        return fragments.stream()
                .reduce((left, right) -> left + " || " + right)
                .orElse("");
    }

    /**
     * Creates an empty context for requests that have no usable memory.
     *
     * @param conversationId conversation identifier to carry forward even when history is absent
     * @return empty memory context
     */
    public static ConversationMemoryContext empty(String conversationId) {
        return new ConversationMemoryContext(
                ConversationMemoryConversation.empty(conversationId),
                ConversationMemorySummary.empty(),
                List.of());
    }

    private static String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 120) {
            return safeValue;
        }
        return safeValue.substring(0, 117) + "...";
    }
}

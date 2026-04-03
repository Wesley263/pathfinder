package com.flightpathfinder.rag.core.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

/**
 * Minimal in-memory fallback for conversation memory.
 *
 * <p>This bean only exists when no persistent store is configured, which keeps local development usable
 * without changing the higher-level memory contract.
 */
@Repository
@ConditionalOnMissingBean(ConversationMemoryStore.class)
public class InMemoryConversationMemoryStore implements ConversationMemoryStore {

    private static final int MAX_STORED_TURNS = 20;

    private final Map<String, Deque<ConversationMemoryTurn>> conversations = new ConcurrentHashMap<>();

    /**
     * Loads recent turns from the in-memory fallback store.
     *
     * @param conversationId stable conversation identifier
     * @param recentTurnLimit number of recent turns requested
     * @return recent-turn snapshot or empty snapshot when no turns are stored
     */
    @Override
    public ConversationMemorySnapshot load(String conversationId, int recentTurnLimit) {
        if (conversationId == null || conversationId.isBlank()) {
            return ConversationMemorySnapshot.empty("");
        }
        Deque<ConversationMemoryTurn> turns = conversations.get(conversationId);
        if (turns == null || turns.isEmpty()) {
            return ConversationMemorySnapshot.empty(conversationId);
        }
        List<ConversationMemoryTurn> storedTurns = new ArrayList<>(turns);
        int fromIndex = Math.max(0, storedTurns.size() - Math.max(1, recentTurnLimit));
        return new ConversationMemorySnapshot(
                new ConversationMemoryConversation(conversationId, "", storedTurns.size(), storedTurns.size() * 2, null, null),
                List.copyOf(storedTurns.subList(fromIndex, storedTurns.size())));
    }

    /**
     * Appends one completed turn to the in-memory fallback store.
     *
     * @param conversationId stable conversation identifier
     * @param turn completed turn to store
     */
    @Override
    public void appendTurn(String conversationId, ConversationMemoryTurn turn) {
        if (conversationId == null || conversationId.isBlank() || turn == null) {
            return;
        }
        conversations.compute(conversationId, (key, existingTurns) -> {
            Deque<ConversationMemoryTurn> turns = existingTurns == null ? new ArrayDeque<>() : new ArrayDeque<>(existingTurns);
            turns.addLast(turn);
            // The fallback store is intentionally bounded so local/dev sessions cannot grow unbounded in heap.
            while (turns.size() > MAX_STORED_TURNS) {
                turns.removeFirst();
            }
            return turns;
        });
    }
}

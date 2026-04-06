package com.flightpathfinder.rag.core.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class ConversationMemorySnapshotTest {

    @Test
    void empty_shouldCreateSnapshotWithConversationIdAndNoTurns() {
        ConversationMemorySnapshot snapshot = ConversationMemorySnapshot.empty("conv-1");

        assertEquals("conv-1", snapshot.conversation().conversationId());
        assertTrue(snapshot.turns().isEmpty());
    }

    @Test
    void constructor_shouldNormalizeNullAndCopyTurns() {
        List<ConversationMemoryTurn> turns = new ArrayList<>();
        turns.add(new ConversationMemoryTurn("req-1", "q", "rq", "a", "SUCCESS", Instant.now()));

        ConversationMemorySnapshot snapshot = new ConversationMemorySnapshot(null, turns);
        turns.clear();

        assertEquals("", snapshot.conversation().conversationId());
        assertEquals(1, snapshot.turns().size());
        assertEquals("req-1", snapshot.turns().getFirst().requestId());
    }
}




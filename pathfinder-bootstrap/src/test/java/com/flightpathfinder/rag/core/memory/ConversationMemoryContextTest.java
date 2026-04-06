package com.flightpathfinder.rag.core.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class ConversationMemoryContextTest {

    @Test
    void empty_shouldReturnTrueAndExposeNoHistory() {
        ConversationMemoryContext context = ConversationMemoryContext.empty("conv-1");

        assertTrue(context.empty());
        assertFalse(context.hasSummary());
        assertEquals("conv-1", context.conversationId());
        assertEquals("", context.routingHistoryText());
    }

    @Test
    void routingHistoryText_shouldIncludeSummaryAndAbbreviatedTurns() {
        String longAnswer = "a".repeat(130);
        ConversationMemoryContext context = new ConversationMemoryContext(
                new ConversationMemoryConversation("conv-2", "req-2", 3, 6, Instant.now(), Instant.now()),
                new ConversationMemorySummary("用户在咨询日本行程", 2, Instant.now()),
                List.of(new ConversationMemoryTurn("req-2", "原问题", "改写问题", longAnswer, "SUCCESS", Instant.now())));

        String history = context.routingHistoryText();

        assertFalse(context.empty());
        assertTrue(context.hasSummary());
        assertTrue(history.startsWith("Summary: 用户在咨询日本行程"));
        assertTrue(history.contains("User: 改写问题 | Assistant: "));
        assertTrue(history.contains("..."));
        assertTrue(history.contains(" || "));
    }
}


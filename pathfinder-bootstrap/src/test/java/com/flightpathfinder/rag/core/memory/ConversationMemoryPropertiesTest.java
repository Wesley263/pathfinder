package com.flightpathfinder.rag.core.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class ConversationMemoryPropertiesTest {

    @Test
    void setters_shouldEnforceMinimumBounds() {
        ConversationMemoryProperties properties = new ConversationMemoryProperties();

        properties.setRecentTurnLimit(0);
        properties.setSummaryStartTurns(-1);
        properties.setSummaryMaxChars(50);

        assertEquals(1, properties.recentTurnLimit());
        assertEquals(1, properties.summaryStartTurns());
        assertEquals(120, properties.summaryMaxChars());
    }

    @Test
    void summaryEnabled_shouldBeConfigurable() {
        ConversationMemoryProperties properties = new ConversationMemoryProperties();

        assertTrue(properties.summaryEnabled());
        properties.setSummaryEnabled(false);
        assertFalse(properties.summaryEnabled());
    }
}




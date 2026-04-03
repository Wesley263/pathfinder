package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

public record ConversationMemorySummary(
        String summaryText,
        int summarizedTurnCount,
        Instant updatedAt) {

    public ConversationMemorySummary {
        summaryText = summaryText == null ? "" : summaryText.trim();
        summarizedTurnCount = Math.max(0, summarizedTurnCount);
    }

    public boolean exists() {
        return !summaryText.isBlank();
    }

    public static ConversationMemorySummary empty() {
        return new ConversationMemorySummary("", 0, null);
    }
}

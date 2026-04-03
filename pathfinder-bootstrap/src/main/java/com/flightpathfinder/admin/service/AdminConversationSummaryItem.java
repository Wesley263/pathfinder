package com.flightpathfinder.admin.service;

import java.time.Instant;

public record AdminConversationSummaryItem(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        boolean hasSummary,
        Instant summaryUpdatedAt,
        Instant createdAt,
        Instant updatedAt) {

    public AdminConversationSummaryItem {
        conversationId = conversationId == null ? "" : conversationId.trim();
        lastRequestId = lastRequestId == null ? "" : lastRequestId.trim();
        turnCount = Math.max(0, turnCount);
        messageCount = Math.max(0, messageCount);
    }
}

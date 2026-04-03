package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

public record AdminConversationSummaryVO(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        boolean hasSummary,
        Instant summaryUpdatedAt,
        Instant createdAt,
        Instant updatedAt) {
}

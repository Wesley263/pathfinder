package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

public record AdminConversationSummaryDetailVO(
        boolean hasSummary,
        String summaryText,
        int summarizedTurnCount,
        Instant updatedAt) {
}

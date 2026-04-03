package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

public record AdminConversationDetailBodyVO(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        boolean hasSummary,
        AdminConversationSummaryDetailVO summary,
        List<AdminConversationMessageVO> recentMessages) {
}

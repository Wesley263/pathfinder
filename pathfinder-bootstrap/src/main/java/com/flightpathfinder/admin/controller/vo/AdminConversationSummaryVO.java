package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
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


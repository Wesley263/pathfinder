package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

/**
 * 管理端服务层数据模型。
 */
public record AdminConversationDetail(
        String conversationId,
        String lastRequestId,
        int turnCount,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        boolean hasSummary,
        AdminConversationSummaryDetail summary,
        List<AdminConversationMessageItem> recentMessages) {

    public AdminConversationDetail {
        conversationId = conversationId == null ? "" : conversationId.trim();
        lastRequestId = lastRequestId == null ? "" : lastRequestId.trim();
        turnCount = Math.max(0, turnCount);
        messageCount = Math.max(0, messageCount);
        recentMessages = List.copyOf(recentMessages == null ? List.of() : recentMessages);
    }
}


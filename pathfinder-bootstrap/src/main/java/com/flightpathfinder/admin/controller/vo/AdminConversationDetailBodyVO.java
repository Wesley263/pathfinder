package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

/**
 * 管理端响应视图模型。
 */
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


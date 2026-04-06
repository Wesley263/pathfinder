package com.flightpathfinder.admin.service;

import java.time.Instant;

/**
 * 管理端服务层数据模型。
 */
public record AdminConversationSummaryDetail(
        boolean hasSummary,
        String summaryText,
        int summarizedTurnCount,
        Instant updatedAt) {

    public AdminConversationSummaryDetail {
        summaryText = summaryText == null ? "" : summaryText.trim();
        summarizedTurnCount = Math.max(0, summarizedTurnCount);
    }
}


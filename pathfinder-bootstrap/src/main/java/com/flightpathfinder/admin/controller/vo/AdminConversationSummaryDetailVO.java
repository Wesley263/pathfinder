package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
public record AdminConversationSummaryDetailVO(
        boolean hasSummary,
        String summaryText,
        int summarizedTurnCount,
        Instant updatedAt) {
}


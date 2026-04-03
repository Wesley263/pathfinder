package com.flightpathfinder.admin.service;

import java.util.List;

public record AdminConversationListResult(
        String status,
        String requestedConversationId,
        String message,
        int limit,
        int count,
        List<AdminConversationSummaryItem> conversations) {

    public AdminConversationListResult {
        status = status == null || status.isBlank() ? "EMPTY" : status.trim();
        requestedConversationId = requestedConversationId == null ? "" : requestedConversationId.trim();
        message = message == null ? "" : message.trim();
        limit = Math.max(1, limit);
        count = Math.max(0, count);
        conversations = List.copyOf(conversations == null ? List.of() : conversations);
    }
}

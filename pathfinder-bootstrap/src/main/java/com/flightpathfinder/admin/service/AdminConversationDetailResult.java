package com.flightpathfinder.admin.service;

public record AdminConversationDetailResult(
        String conversationId,
        String status,
        String message,
        AdminConversationDetail detail) {

    public AdminConversationDetailResult {
        conversationId = conversationId == null ? "" : conversationId.trim();
        status = status == null || status.isBlank() ? "NOT_FOUND" : status.trim();
        message = message == null ? "" : message.trim();
    }
}

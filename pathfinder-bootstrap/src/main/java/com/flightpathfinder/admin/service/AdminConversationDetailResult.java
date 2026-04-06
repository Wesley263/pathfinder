package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
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


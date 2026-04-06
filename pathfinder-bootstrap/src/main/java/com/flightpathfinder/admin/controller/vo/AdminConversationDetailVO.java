package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminConversationDetailVO(
        String conversationId,
        String status,
        String message,
        AdminConversationDetailBodyVO detail) {
}


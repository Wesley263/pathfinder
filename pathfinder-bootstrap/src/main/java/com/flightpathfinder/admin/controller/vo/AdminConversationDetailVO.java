package com.flightpathfinder.admin.controller.vo;

public record AdminConversationDetailVO(
        String conversationId,
        String status,
        String message,
        AdminConversationDetailBodyVO detail) {
}

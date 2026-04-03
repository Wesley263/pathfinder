package com.flightpathfinder.admin.controller.vo;

import java.util.List;

public record AdminConversationListVO(
        String status,
        String requestedConversationId,
        String message,
        int limit,
        int count,
        List<AdminConversationSummaryVO> conversations) {
}

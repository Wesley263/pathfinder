package com.flightpathfinder.admin.controller.vo;

import java.util.List;

/**
 * 管理端响应视图模型。
 */
public record AdminConversationListVO(
        String status,
        String requestedConversationId,
        String message,
        int limit,
        int count,
        List<AdminConversationSummaryVO> conversations) {
}


package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminMcpToolDetailResult(
        String toolId,
        String status,
        String message,
        AdminMcpToolDetail detail) {

    public AdminMcpToolDetailResult {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "NOT_FOUND" : status.trim();
        message = message == null ? "" : message.trim();
    }
}


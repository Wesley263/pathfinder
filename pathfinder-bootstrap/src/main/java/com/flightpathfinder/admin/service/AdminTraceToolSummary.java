package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminTraceToolSummary(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss,
        boolean error) {

    public AdminTraceToolSummary {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
    }
}


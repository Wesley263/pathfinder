package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminTraceToolVO(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss,
        boolean error) {
}


package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminMcpToolDetailVO(
        String toolId,
        String status,
        String message,
        AdminMcpToolBodyVO detail) {
}


package com.flightpathfinder.admin.controller.vo;

public record AdminMcpToolDetailVO(
        String toolId,
        String status,
        String message,
        AdminMcpToolBodyVO detail) {
}

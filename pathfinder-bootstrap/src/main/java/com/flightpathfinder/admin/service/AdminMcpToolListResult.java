package com.flightpathfinder.admin.service;

import java.util.List;

public record AdminMcpToolListResult(
        String status,
        String message,
        int count,
        int availableCount,
        List<AdminMcpToolSummaryItem> tools) {

    public AdminMcpToolListResult {
        status = status == null || status.isBlank() ? "SUCCESS" : status.trim();
        message = message == null ? "" : message.trim();
        count = Math.max(0, count);
        availableCount = Math.max(0, availableCount);
        tools = List.copyOf(tools == null ? List.of() : tools);
    }
}

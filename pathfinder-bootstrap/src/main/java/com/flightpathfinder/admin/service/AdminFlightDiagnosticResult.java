package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

/**
 * 管理端服务层数据模型。
 */
public record AdminFlightDiagnosticResult(
        String toolId,
        boolean toolAvailable,
        boolean success,
        String status,
        String message,
        String error,
        boolean retryable,
        String suggestedAction,
        String origin,
        String destination,
        String date,
        int flexibilityDays,
        int topK,
        int flightCount,
        Instant checkedAt,
        List<AdminFlightDiagnosticOption> flights) {

    public AdminFlightDiagnosticResult {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
        error = error == null ? "" : error.trim();
        suggestedAction = suggestedAction == null ? "" : suggestedAction.trim();
        origin = origin == null ? "" : origin.trim();
        destination = destination == null ? "" : destination.trim();
        date = date == null ? "" : date.trim();
        flightCount = Math.max(0, flightCount);
        flights = List.copyOf(flights == null ? List.of() : flights);
    }
}


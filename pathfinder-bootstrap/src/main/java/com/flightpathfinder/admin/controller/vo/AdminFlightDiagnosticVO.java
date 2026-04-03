package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

public record AdminFlightDiagnosticVO(
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
        List<AdminFlightDiagnosticOptionVO> flights) {
}

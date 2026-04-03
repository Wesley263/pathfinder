package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

public record AdminPathDiagnosticVO(
        String toolId,
        boolean toolAvailable,
        boolean success,
        String status,
        String message,
        String error,
        boolean snapshotMiss,
        boolean retryable,
        String suggestedAction,
        String graphKey,
        String snapshotVersion,
        String schemaVersion,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK,
        int pathCount,
        Instant checkedAt,
        List<AdminPathDiagnosticCandidateVO> paths) {
}

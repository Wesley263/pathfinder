package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

public record AdminPathDiagnosticResult(
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
        List<AdminPathDiagnosticCandidate> paths) {

    public AdminPathDiagnosticResult {
        toolId = toolId == null ? "" : toolId.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        message = message == null ? "" : message.trim();
        error = error == null ? "" : error.trim();
        suggestedAction = suggestedAction == null ? "" : suggestedAction.trim();
        graphKey = graphKey == null ? "" : graphKey.trim();
        snapshotVersion = snapshotVersion == null ? "" : snapshotVersion.trim();
        schemaVersion = schemaVersion == null ? "" : schemaVersion.trim();
        origin = origin == null ? "" : origin.trim();
        destination = destination == null ? "" : destination.trim();
        pathCount = Math.max(0, pathCount);
        paths = List.copyOf(paths == null ? List.of() : paths);
    }
}

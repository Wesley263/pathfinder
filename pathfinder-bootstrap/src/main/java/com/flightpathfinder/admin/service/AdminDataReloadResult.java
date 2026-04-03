package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

public record AdminDataReloadResult(
        String graphKey,
        String overallStatus,
        String reason,
        Instant reloadedAt,
        List<AdminDatasetReloadResult> datasetResults,
        AdminDataStats stats,
        List<String> cacheInvalidatedScopes,
        boolean graphSnapshotInvalidated) {
}

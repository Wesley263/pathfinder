package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

/**
 * 管理端服务层数据模型。
 */
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


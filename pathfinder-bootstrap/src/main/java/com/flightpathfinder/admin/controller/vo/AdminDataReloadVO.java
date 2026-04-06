package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

/**
 * 管理端响应视图模型。
 */
public record AdminDataReloadVO(
        String graphKey,
        String overallStatus,
        String reason,
        Instant reloadedAt,
        List<AdminDatasetReloadVO> datasetResults,
        AdminDataStatsVO stats,
        List<String> cacheInvalidatedScopes,
        boolean graphSnapshotInvalidated) {
}


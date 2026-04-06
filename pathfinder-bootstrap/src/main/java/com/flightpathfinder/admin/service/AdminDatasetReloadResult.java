package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 管理端服务层数据模型。
 */
public record AdminDatasetReloadResult(
        String datasetId,
        String status,
        String reason,
        List<String> sourceLocations,
        long processedCount,
        long upsertedCount,
        long failedCount,
        Instant startedAt,
        Instant completedAt,
        Map<String, Object> details) {
}


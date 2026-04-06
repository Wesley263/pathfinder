package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 管理端响应视图模型。
 */
public record AdminDatasetReloadVO(
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


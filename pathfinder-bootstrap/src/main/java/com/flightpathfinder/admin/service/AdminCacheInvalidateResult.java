package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

/**
 * 管理端服务层数据模型。
 */
public record AdminCacheInvalidateResult(
        String graphKey,
        String status,
        String reason,
        Instant invalidatedAt,
        long clearedGraphSnapshotKeyCount,
        int clearedMcpToolCount,
        List<String> clearedScopes) {
}


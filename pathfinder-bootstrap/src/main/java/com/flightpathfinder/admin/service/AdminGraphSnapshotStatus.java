package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.Map;

/**
 * 管理端服务层数据模型。
 */
public record AdminGraphSnapshotStatus(
        String graphKey,
        String status,
        String snapshotVersion,
        String schemaVersion,
        Instant generatedAt,
        Instant observedAt,
        int nodeCount,
        int edgeCount,
        String sourceFingerprint,
        String reason,
        Map<String, Object> metadata) {
}


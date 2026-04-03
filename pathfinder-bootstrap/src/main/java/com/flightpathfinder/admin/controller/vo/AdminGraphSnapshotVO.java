package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.Map;

public record AdminGraphSnapshotVO(
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

package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.Map;

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

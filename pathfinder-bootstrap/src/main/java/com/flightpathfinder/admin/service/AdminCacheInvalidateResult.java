package com.flightpathfinder.admin.service;

import java.time.Instant;
import java.util.List;

public record AdminCacheInvalidateResult(
        String graphKey,
        String status,
        String reason,
        Instant invalidatedAt,
        long clearedGraphSnapshotKeyCount,
        int clearedMcpToolCount,
        List<String> clearedScopes) {
}

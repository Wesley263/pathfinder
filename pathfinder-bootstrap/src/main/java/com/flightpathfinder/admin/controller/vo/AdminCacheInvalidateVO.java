package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;
import java.util.List;

public record AdminCacheInvalidateVO(
        String graphKey,
        String status,
        String reason,
        Instant invalidatedAt,
        long clearedGraphSnapshotKeyCount,
        int clearedMcpToolCount,
        List<String> clearedScopes) {
}

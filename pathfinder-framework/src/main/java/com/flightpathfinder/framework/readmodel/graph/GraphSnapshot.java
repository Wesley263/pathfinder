package com.flightpathfinder.framework.readmodel.graph;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GraphSnapshot(
        String schemaVersion,
        String snapshotVersion,
        Instant generatedAt,
        String graphKey,
        String sourceFingerprint,
        List<GraphSnapshotNode> nodes,
        List<GraphSnapshotEdge> edges,
        Map<String, Object> metadata) {
}


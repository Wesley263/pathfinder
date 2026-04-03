package com.flightpathfinder.framework.readmodel.graph;

import java.time.Duration;

public final class GraphSnapshotRedisKeys {

    public static final String DEFAULT_GRAPH_KEY = "flight-main";
    public static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private GraphSnapshotRedisKeys() {
    }

    public static String latestVersionKey(String graphKey) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":latest";
    }

    public static String snapshotKey(String graphKey, String snapshotVersion) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":version:" + snapshotVersion;
    }

    public static String snapshotKeyPattern(String graphKey) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":*";
    }

    private static String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank() ? DEFAULT_GRAPH_KEY : graphKey;
    }
}

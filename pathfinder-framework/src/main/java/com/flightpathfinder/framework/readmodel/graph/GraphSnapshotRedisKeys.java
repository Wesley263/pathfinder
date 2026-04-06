package com.flightpathfinder.framework.readmodel.graph;

import java.time.Duration;

/**
 * 图快照 Redis 键规范。
 *
 * 统一管理 latest 指针、版本快照以及批量失效使用的键模式。
 */
public final class GraphSnapshotRedisKeys {

    /** 默认图标识。 */
    public static final String DEFAULT_GRAPH_KEY = "flight-main";
    /** 图快照默认 TTL。 */
    public static final Duration DEFAULT_TTL = Duration.ofDays(30);

    /** 工具类禁止实例化。 */
    private GraphSnapshotRedisKeys() {
    }

    /**
     * 生成 latest 版本指针键。
     *
     * @param graphKey 图标识
     * @return latest 版本指针键
     */
    public static String latestVersionKey(String graphKey) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":latest";
    }

    /**
     * 生成版本快照键。
     *
     * @param graphKey 图标识
     * @param snapshotVersion 快照版本
     * @return 快照键
     */
    public static String snapshotKey(String graphKey, String snapshotVersion) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":version:" + snapshotVersion;
    }

    /**
     * 生成快照键模式。
     *
     * @param graphKey 图标识
     * @return 快照键匹配模式
     */
    public static String snapshotKeyPattern(String graphKey) {
        return "graph:snapshot:" + normalizeGraphKey(graphKey) + ":*";
    }

    /**
     * 归一化图标识。
     *
     * @param graphKey 原始图标识
     * @return 归一化后的图标识
     */
    private static String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank() ? DEFAULT_GRAPH_KEY : graphKey;
    }
}

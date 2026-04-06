package com.flightpathfinder.framework.readmodel.graph;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 图快照读模型顶层对象。
 *
 * 一个快照代表某个图标识在某一时刻的完整节点与边集合。
 * 快照对象会在主程序发布到 Redis 后，由查询与搜索流程消费。
 *
 * @param schemaVersion 快照 schema 版本，用于兼容校验
 * @param snapshotVersion 快照实例版本
 * @param generatedAt 快照生成时间
 * @param graphKey 图标识
 * @param sourceFingerprint 数据源指纹
 * @param nodes 节点列表
 * @param edges 边列表
 * @param metadata 元数据
 */
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


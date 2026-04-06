package com.flightpathfinder.framework.readmodel.graph;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 图快照读模型顶层对象。
 *
 * 说明。
 * 说明。
 *
 * @param schemaVersion 参数说明。
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


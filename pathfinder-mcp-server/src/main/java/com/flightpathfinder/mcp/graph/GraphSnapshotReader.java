package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * MCP server 侧图快照只读加载边界。
 *
 * <p>MCP server 仅消费已发布快照，不拥有图构建职责，
 * 从而与 bootstrap 侧数据库和重建流程解耦。</p>
 */
public interface GraphSnapshotReader {

    /**
     * 加载指定 graphKey 的最新已发布快照。
     *
     * @param graphKey 图逻辑标识
     * @return 最新快照（存在时返回）
     */
    Optional<GraphSnapshot> loadLatest(String graphKey);
}

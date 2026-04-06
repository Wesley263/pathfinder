package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface GraphSnapshotReader {

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @return 最新快照（存在时返回）
     */
    Optional<GraphSnapshot> loadLatest(String graphKey);
}

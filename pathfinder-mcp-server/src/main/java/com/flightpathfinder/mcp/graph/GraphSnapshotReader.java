package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * 图快照读取接口。
 *
 * 抽象最新快照读取能力，供路径搜索执行器解耦底层存储实现。
 */
public interface GraphSnapshotReader {

    /**
     * 加载指定图键的最新快照。
     *
     * @param graphKey 图逻辑标识
     * @return 最新快照（存在时返回）
     */
    Optional<GraphSnapshot> loadLatest(String graphKey);
}


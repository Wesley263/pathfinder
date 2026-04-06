package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照构建边界。
 *
 * 说明。
 * 说明。
 */
public interface GraphSnapshotBuilder {

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @return 完整物化后的图快照读模型
     */
    GraphSnapshot build(String graphKey);
}

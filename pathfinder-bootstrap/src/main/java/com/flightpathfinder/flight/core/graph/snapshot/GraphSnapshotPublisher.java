package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照发布边界。
 *
 * 说明。
 */
public interface GraphSnapshotPublisher {

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @param snapshot 待发布快照载荷
     */
    void publish(String graphKey, GraphSnapshot snapshot);
}

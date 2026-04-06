package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照发布边界。
 *
 * 发布器负责写入快照载荷并更新当前版本指针。
 */
public interface GraphSnapshotPublisher {

    /**
     * 发布指定图标识的快照。
     *
     * @param graphKey 图逻辑标识
     * @param snapshot 待发布快照载荷
     */
    void publish(String graphKey, GraphSnapshot snapshot);
}

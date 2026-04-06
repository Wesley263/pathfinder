package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照发布边界。
 *
 * <p>发布与构建分离，便于主程序独立演进存储机制，而不影响快照内容装配逻辑。</p>
 */
public interface GraphSnapshotPublisher {

    /**
     * 为指定 graphKey 发布快照。
     *
     * @param graphKey 图逻辑标识
     * @param snapshot 待发布快照载荷
     */
    void publish(String graphKey, GraphSnapshot snapshot);
}

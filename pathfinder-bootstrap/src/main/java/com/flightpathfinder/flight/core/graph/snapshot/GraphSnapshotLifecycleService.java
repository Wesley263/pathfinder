package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 主应用侧图快照生命周期操作边界。
 *
 * 说明。
 * 说明。
 */
public interface GraphSnapshotLifecycleService {

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     */
    void invalidate(String graphKey);

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @param reason 本次重建的运维原因
     * @return 已发布的重建快照
     */
    GraphSnapshot rebuild(String graphKey, String reason);
}


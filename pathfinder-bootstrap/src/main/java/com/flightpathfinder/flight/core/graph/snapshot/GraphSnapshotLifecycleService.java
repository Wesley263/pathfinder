package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 主应用侧图快照生命周期操作边界。
 *
 * 对外提供失效与重建能力，主要用于管理面和运维流程。
 */
public interface GraphSnapshotLifecycleService {

    /**
     * 失效指定图标识下的所有快照键。
     *
     * @param graphKey 图逻辑标识
     */
    void invalidate(String graphKey);

    /**
     * 重建并发布指定图标识的快照。
     *
     * @param graphKey 图逻辑标识
     * @param reason 本次重建的运维原因
     * @return 已发布的重建快照
     */
    GraphSnapshot rebuild(String graphKey, String reason);
}


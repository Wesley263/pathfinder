package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 主应用侧图快照生命周期操作边界。
 *
 * <p>该边界让 admin 与业务流程可以直接触发失效和重建，
 * 而不需要了解快照如何构建或如何存储。</p>
 */
public interface GraphSnapshotLifecycleService {

    /**
     * 失效一个 graphKey 的已发布快照状态。
     *
     * @param graphKey 图逻辑标识
     */
    void invalidate(String graphKey);

    /**
     * 重建并重新发布一个 graphKey 的当前快照。
     *
     * @param graphKey 图逻辑标识
     * @param reason 本次重建的运维原因
     * @return 已发布的重建快照
     */
    GraphSnapshot rebuild(String graphKey, String reason);
}


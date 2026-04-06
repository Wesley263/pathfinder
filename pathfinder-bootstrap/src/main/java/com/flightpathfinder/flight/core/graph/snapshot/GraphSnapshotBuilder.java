package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照构建边界。
 *
 * 实现方通常从主业务库读取机场与航线数据，构建可发布的
 * 不可变 GraphSnapshot 读模型。
 */
public interface GraphSnapshotBuilder {

    /**
     * 构建指定图标识的完整快照。
     *
     * @param graphKey 图逻辑标识
     * @return 完整物化后的图快照读模型
     */
    GraphSnapshot build(String graphKey);
}

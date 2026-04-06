package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * 图快照构建边界。
 *
 * <p>构建器位于 bootstrap 主程序侧，因为图数据生命周期由主程序负责，
 * 何时生成新快照也应由主程序决策。</p>
 */
public interface GraphSnapshotBuilder {

    /**
     * 为指定 graphKey 构建新快照。
     *
     * @param graphKey 图逻辑标识
     * @return 完整物化后的图快照读模型
     */
    GraphSnapshot build(String graphKey);
}

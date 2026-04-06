package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * 当前已发布图快照的只读查询边界。
 *
 * <p>该服务用于 admin/巡检场景，只暴露当前快照视图，不向调用方泄漏底层存储细节。</p>
 */
public interface GraphSnapshotQueryService {

    /**
     * 加载指定 graphKey 的当前已发布快照。
     *
     * @param graphKey 图逻辑标识
     * @return 当前快照（存在时返回）
     */
    Optional<GraphSnapshot> loadCurrent(String graphKey);
}

package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * 当前已发布图快照的只读查询边界。
 *
 * 说明。
 */
public interface GraphSnapshotQueryService {

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @return 当前快照（存在时返回）
     */
    Optional<GraphSnapshot> loadCurrent(String graphKey);
}

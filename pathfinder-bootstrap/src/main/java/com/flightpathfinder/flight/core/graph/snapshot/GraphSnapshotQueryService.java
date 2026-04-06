package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * 当前已发布图快照的只读查询边界。
 *
 * 查询实现通常从 Redis 读取 latest 版本并反序列化快照载荷。
 */
public interface GraphSnapshotQueryService {

    /**
     * 加载指定图标识的当前已发布快照。
     *
     * @param graphKey 图逻辑标识
     * @return 当前快照（存在时返回）
     */
    Optional<GraphSnapshot> loadCurrent(String graphKey);
}

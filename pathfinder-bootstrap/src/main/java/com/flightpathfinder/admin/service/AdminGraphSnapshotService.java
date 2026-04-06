package com.flightpathfinder.admin.service;

/**
 * 图快照管理的管理端服务。
 *
 * <p>图快照生命周期与通用数据重载语义不同，
 * 因此以独立管理能力暴露。
 */
public interface AdminGraphSnapshotService {

    /**
        * 加载指定图键当前已发布的快照状态。
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @return current snapshot status, including structured miss information when absent
     */
    AdminGraphSnapshotStatus currentSnapshot(String graphKey);

    /**
        * 重建指定图键的已发布图快照。
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied rebuild reason
     * @return rebuilt snapshot status
     */
    AdminGraphSnapshotStatus rebuild(String graphKey, String reason);

    /**
        * 失效指定图键当前已发布的图快照。
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied invalidation reason
     * @return invalidation status summary
     */
    AdminGraphSnapshotStatus invalidate(String graphKey, String reason);
}

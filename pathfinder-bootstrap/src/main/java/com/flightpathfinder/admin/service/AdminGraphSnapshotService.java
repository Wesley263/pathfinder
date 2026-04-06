package com.flightpathfinder.admin.service;

/**
 * 图快照管理的管理端服务。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 * 因此以独立管理能力暴露。
 */
public interface AdminGraphSnapshotService {

    /**
        * 加载指定图键当前已发布的快照状态。
     *
     * @param graphKey 参数说明。
     * @return 返回结果。
     */
    AdminGraphSnapshotStatus currentSnapshot(String graphKey);

    /**
        * 重建指定图键的已发布图快照。
     *
     * @param graphKey 参数说明。
     * @param reason 参数说明。
     * @return 返回结果。
     */
    AdminGraphSnapshotStatus rebuild(String graphKey, String reason);

    /**
        * 失效指定图键当前已发布的图快照。
     *
     * @param graphKey 参数说明。
     * @param reason 参数说明。
     * @return 返回结果。
     */
    AdminGraphSnapshotStatus invalidate(String graphKey, String reason);
}



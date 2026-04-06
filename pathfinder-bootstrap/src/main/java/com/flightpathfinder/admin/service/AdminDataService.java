package com.flightpathfinder.admin.service;

/**
 * 管理端数据 ETL、统计与缓存维护编排服务。
 *
 * <p>该服务直接操作与巡检后端数据资产，属于管理端能力，
 * 不参与用户侧问答流程。
 */
public interface AdminDataService {

    /**
        * 触发外部数据集 ETL 重载。
     *
     * @param graphKey graph key whose downstream graph snapshot should be invalidated if data changes
     * @param reason operator-supplied reason for the reload
     * @return dataset-level reload result plus current aggregate stats
     */
    AdminDataReloadResult reload(String graphKey, String reason);

    /**
        * 返回管理端数据表当前聚合计数。
     *
     * @return current data statistics snapshot
     */
    AdminDataStats currentStats();

    /**
        * 失效依赖导入数据的缓存。
     *
     * @param graphKey graph key whose published snapshot should be invalidated
     * @param reason operator-supplied reason for invalidation
     * @return cache invalidation summary
     */
    AdminCacheInvalidateResult invalidateCaches(String graphKey, String reason);
}

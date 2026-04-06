package com.flightpathfinder.admin.service;

/**
 * 说明。
 *
 * 说明。
 * 不参与用户侧问答流程。
 */
public interface AdminDataService {

    /**
        * 说明。
     *
     * @param graphKey 参数说明。
     * @param reason 参数说明。
     * @return 返回结果。
     */
    AdminDataReloadResult reload(String graphKey, String reason);

    /**
        * 返回管理端数据表当前聚合计数。
     *
     * @return 返回结果。
     */
    AdminDataStats currentStats();

    /**
        * 失效依赖导入数据的缓存。
     *
     * @param graphKey 参数说明。
     * @param reason 参数说明。
     * @return 返回结果。
     */
    AdminCacheInvalidateResult invalidateCaches(String graphKey, String reason);
}

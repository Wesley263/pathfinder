package com.flightpathfinder.admin.service;

/**
 * 管理端数据维护服务抽象。
 *
 * 负责外部数据重载、统计查询与缓存失效能力，
 * 不参与用户侧问答流程。
 */
public interface AdminDataService {

    /**
          * 执行管理端数据重载。
     *
      * @param graphKey 图标识
      * @param reason 重载原因
      * @return 重载结果
     */
    AdminDataReloadResult reload(String graphKey, String reason);

    /**
        * 返回管理端数据表当前聚合计数。
     *
     * @return 数据统计快照
     */
    AdminDataStats currentStats();

    /**
        * 失效依赖导入数据的缓存。
     *
     * @param graphKey 图标识
     * @param reason 失效原因
     * @return 缓存失效结果
     */
    AdminCacheInvalidateResult invalidateCaches(String graphKey, String reason);
}


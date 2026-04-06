package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.service.AdminDatasetReloadResult;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface AdminDatasetImporter {

    /**
        * 返回管理端重载报告使用的稳定数据集标识。
     *
     * @return 返回结果。
     */
    String datasetId();

    /**
        * 说明。
     *
     * @return 返回结果。
     */
    AdminDatasetReloadResult reload();
}

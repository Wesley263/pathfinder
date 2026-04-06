package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.service.AdminDatasetReloadResult;

/**
 * 管理端数据集导入器抽象。
 *
 * 统一定义数据集标识与重载入口，便于编排器按顺序触发导入。
 */
public interface AdminDatasetImporter {

    /**
        * 返回管理端重载报告使用的稳定数据集标识。
     *
      * @return 数据集唯一标识
     */
    String datasetId();

    /**
          * 执行当前数据集的重载流程。
     *
      * @return 数据集级别的重载结果
     */
    AdminDatasetReloadResult reload();
}


package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.service.AdminDatasetReloadResult;

/**
 * 管理端触发重载时使用的数据集级 ETL 导入器。
 *
 * <p>每个导入器负责一个外部数据集，使管理端重载能够以数据集粒度报告结果，
 * 而不是把所有 ETL 工作压缩成单一的成功/失败标记。
 */
public interface AdminDatasetImporter {

    /**
        * 返回管理端重载报告使用的稳定数据集标识。
     *
     * @return dataset id such as {@code openflights}, {@code visa}, or {@code city_cost}
     */
    String datasetId();

    /**
        * 执行单个数据集的 ETL 导入。
     *
     * @return dataset-level reload result including processed/upserted/failed counts and source details
     */
    AdminDatasetReloadResult reload();
}

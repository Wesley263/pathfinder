package com.flightpathfinder.admin.service;

/**
 * Admin-facing orchestration service for data ETL, stats and cache maintenance.
 *
 * <p>This service belongs to the management surface because these operations change or inspect backend data
 * assets directly and are not part of the user-facing question-answering flow.
 */
public interface AdminDataService {

    /**
     * Triggers external dataset ETL reload.
     *
     * @param graphKey graph key whose downstream graph snapshot should be invalidated if data changes
     * @param reason operator-supplied reason for the reload
     * @return dataset-level reload result plus current aggregate stats
     */
    AdminDataReloadResult reload(String graphKey, String reason);

    /**
     * Returns current aggregate counts for admin-managed data tables.
     *
     * @return current data statistics snapshot
     */
    AdminDataStats currentStats();

    /**
     * Invalidates caches that depend on imported data.
     *
     * @param graphKey graph key whose published snapshot should be invalidated
     * @param reason operator-supplied reason for invalidation
     * @return cache invalidation summary
     */
    AdminCacheInvalidateResult invalidateCaches(String graphKey, String reason);
}

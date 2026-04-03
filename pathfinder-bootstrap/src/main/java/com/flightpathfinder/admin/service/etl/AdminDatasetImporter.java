package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.service.AdminDatasetReloadResult;

/**
 * Dataset-level ETL importer used by admin-triggered reload.
 *
 * <p>Each importer owns one external dataset so admin reload can report dataset-granular results instead of
 * collapsing all ETL work into one opaque success/failure bit.
 */
public interface AdminDatasetImporter {

    /**
     * Returns the stable dataset id used in admin reload reporting.
     *
     * @return dataset id such as {@code openflights}, {@code visa}, or {@code city_cost}
     */
    String datasetId();

    /**
     * Executes ETL import for one dataset.
     *
     * @return dataset-level reload result including processed/upserted/failed counts and source details
     */
    AdminDatasetReloadResult reload();
}

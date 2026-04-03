package com.flightpathfinder.admin.service;

/**
 * Admin-facing service for graph snapshot management.
 *
 * <p>Snapshot lifecycle is exposed separately from generic data reload because graph rebuild and invalidation
 * are operational concerns with different semantics from ETL import.
 */
public interface AdminGraphSnapshotService {

    /**
     * Loads the currently published snapshot status for a graph key.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @return current snapshot status, including structured miss information when absent
     */
    AdminGraphSnapshotStatus currentSnapshot(String graphKey);

    /**
     * Rebuilds the published graph snapshot for a graph key.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied rebuild reason
     * @return rebuilt snapshot status
     */
    AdminGraphSnapshotStatus rebuild(String graphKey, String reason);

    /**
     * Invalidates the currently published graph snapshot for a graph key.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied invalidation reason
     * @return invalidation status summary
     */
    AdminGraphSnapshotStatus invalidate(String graphKey, String reason);
}

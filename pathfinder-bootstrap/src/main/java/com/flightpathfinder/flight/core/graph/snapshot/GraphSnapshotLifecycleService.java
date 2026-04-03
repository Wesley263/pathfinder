package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * Owns graph snapshot lifecycle operations on the bootstrap side.
 *
 * <p>This boundary exists so admin and business flows can invalidate or rebuild snapshots
 * without learning how snapshots are built or stored.</p>
 */
public interface GraphSnapshotLifecycleService {

    /**
     * Invalidates the published snapshot state for one graph key.
     *
     * @param graphKey logical graph identifier
     */
    void invalidate(String graphKey);

    /**
     * Rebuilds and republishes the current snapshot for one graph key.
     *
     * @param graphKey logical graph identifier
     * @param reason operational reason recorded into snapshot metadata
     * @return rebuilt snapshot that was published
     */
    GraphSnapshot rebuild(String graphKey, String reason);
}

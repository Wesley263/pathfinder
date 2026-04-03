package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * Publishes graph snapshots to the shared read-model store.
 *
 * <p>Publishing is separate from building so the main program can evolve storage mechanics
 * without changing how the snapshot content itself is assembled.</p>
 */
public interface GraphSnapshotPublisher {

    /**
     * Publishes a snapshot for the supplied graph key.
     *
     * @param graphKey logical graph identifier
     * @param snapshot snapshot payload to publish
     */
    void publish(String graphKey, GraphSnapshot snapshot);
}

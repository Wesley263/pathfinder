package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;

/**
 * Builds a graph snapshot read model from the main program's authoritative data sources.
 *
 * <p>The builder stays on the bootstrap side because the main program owns graph data
 * lifecycle and decides when a new snapshot should exist.</p>
 */
public interface GraphSnapshotBuilder {

    /**
     * Builds a fresh snapshot for the requested graph key.
     *
     * @param graphKey logical graph identifier
     * @return fully materialized graph snapshot read model
     */
    GraphSnapshot build(String graphKey);
}

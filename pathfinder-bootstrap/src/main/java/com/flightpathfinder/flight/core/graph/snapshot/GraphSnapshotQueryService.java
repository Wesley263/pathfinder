package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * Read-only query boundary for the currently published graph snapshot.
 *
 * <p>This service supports admin/inspection use cases without exposing storage details to
 * callers that only need the current published snapshot view.</p>
 */
public interface GraphSnapshotQueryService {

    /**
     * Loads the currently published snapshot for the supplied graph key.
     *
     * @param graphKey logical graph identifier
     * @return current snapshot when present
     */
    Optional<GraphSnapshot> loadCurrent(String graphKey);
}

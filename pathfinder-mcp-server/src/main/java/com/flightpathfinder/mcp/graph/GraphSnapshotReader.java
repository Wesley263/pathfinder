package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.util.Optional;

/**
 * Read-only snapshot loading boundary on the MCP server side.
 *
 * <p>The MCP server consumes published graph snapshots instead of owning graph construction,
 * which keeps the server isolated from bootstrap-side DB and rebuild responsibilities.</p>
 */
public interface GraphSnapshotReader {

    /**
     * Loads the latest published snapshot for the supplied graph key.
     *
     * @param graphKey logical graph identifier
     * @return latest snapshot when present
     */
    Optional<GraphSnapshot> loadLatest(String graphKey);
}

package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import java.util.List;

/**
 * Search boundary for restored graph path planning.
 *
 * <p>The search service operates on the restored in-memory graph only. Snapshot loading and
 * MCP protocol handling stay outside this boundary.</p>
 */
public interface GraphPathSearchService {

    /**
     * Searches candidate paths on the restored graph.
     *
     * @param graph restored graph derived from the published snapshot
     * @param request path search constraints
     * @return ranked candidate paths compatible with the current MCP contract
     */
    List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request);
}

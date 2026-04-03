package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Restores an in-memory search graph from the published snapshot read model.
 *
 * <p>This conversion is separate from snapshot reading so the MCP server can keep transport/
 * storage concerns apart from the search engine's in-memory graph shape.</p>
 */
@Component
public class GraphSnapshotRestorer {

    /**
     * Restores a search-ready graph from a published snapshot payload.
     *
     * @param snapshot published graph snapshot read model
     * @return in-memory graph representation used by path search
     */
    public RestoredFlightGraph restore(GraphSnapshot snapshot) {
        RestoredFlightGraph.Builder builder = RestoredFlightGraph.builder();
        Set<String> nodeIds = new HashSet<>();
        snapshot.nodes().forEach(node -> builder.addNode(new RestoredGraphNode(
                node.airportCode(),
                node.airportName(),
                node.cityName(),
                node.countryCode(),
                node.latitude(),
                node.longitude(),
                node.timezone(),
                node.minTransferMinutes()
        )));
        snapshot.nodes().forEach(node -> nodeIds.add(node.airportCode()));

        for (GraphSnapshotEdge edge : snapshot.edges()) {
            // Only edges whose endpoints survive restoration are admitted so snapshot drift or
            // partial data never leaks broken references into the search graph.
            if (!nodeIds.contains(edge.fromNodeId()) || !nodeIds.contains(edge.toNodeId())) {
                continue;
            }
            builder.addEdge(new RestoredGraphEdge(
                    edge.edgeId(),
                    edge.fromNodeId(),
                    edge.toNodeId(),
                    edge.carrierCode(),
                    edge.carrierName(),
                    edge.carrierType(),
                    edge.basePriceCny(),
                    edge.durationMinutes(),
                    edge.distanceKm(),
                    edge.onTimeRate(),
                    edge.baggageIncluded(),
                    edge.competitionCount(),
                    edge.stops()
            ));
        }

        return builder.build();
    }
}

package com.flightpathfinder.mcp.graph.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RestoredFlightGraph {

    private final Map<String, RestoredGraphNode> nodes;
    private final Map<String, Map<String, List<RestoredGraphEdge>>> edges;

    public RestoredFlightGraph(Map<String, RestoredGraphNode> nodes,
                               Map<String, Map<String, List<RestoredGraphEdge>>> edges) {
        this.nodes = Map.copyOf(nodes);
        this.edges = Map.copyOf(edges);
    }

    public boolean hasNode(String airportCode) {
        return nodes.containsKey(airportCode);
    }

    public RestoredGraphNode getNode(String airportCode) {
        return nodes.get(airportCode);
    }

    public Set<String> getAdjacentNodes(String airportCode) {
        return edges.getOrDefault(airportCode, Map.of()).keySet();
    }

    public List<RestoredGraphEdge> getEdges(String origin, String destination) {
        return edges.getOrDefault(origin, Map.of()).getOrDefault(destination, List.of());
    }

    public Set<String> getAllNodes() {
        return nodes.keySet();
    }

    public List<RestoredGraphEdge> getOutgoingEdges(String airportCode) {
        return edges.getOrDefault(airportCode, Map.of()).values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, RestoredGraphNode> nodes = new HashMap<>();
        private final Map<String, Map<String, List<RestoredGraphEdge>>> edges = new HashMap<>();

        public Builder addNode(RestoredGraphNode node) {
            nodes.put(node.airportCode(), node);
            edges.putIfAbsent(node.airportCode(), new HashMap<>());
            return this;
        }

        public Builder addEdge(RestoredGraphEdge edge) {
            edges.computeIfAbsent(edge.origin(), ignored -> new HashMap<>())
                    .computeIfAbsent(edge.destination(), ignored -> new java.util.ArrayList<>())
                    .add(edge);
            return this;
        }

        public RestoredFlightGraph build() {
            return new RestoredFlightGraph(nodes, edges);
        }
    }
}

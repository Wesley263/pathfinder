package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import com.flightpathfinder.mcp.graph.GraphSnapshotReader;
import com.flightpathfinder.mcp.graph.GraphSnapshotRestorer;
import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.search.GraphPathSearchRequest;
import com.flightpathfinder.mcp.graph.search.GraphPathSearchService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Server-side executor for {@code graph.path.search}.
 *
 * <p>This executor is the bridge between MCP protocol calls and the graph snapshot search pipeline. It
 * stays in {@code pathfinder-mcp-server} because path search must run against the Redis-backed read model
 * inside the server process rather than reusing bootstrap-side graph builders.
 */
@Component
public class GraphPathMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "graph.path.search";
    private static final String SNAPSHOT_MISS_MESSAGE = "graph snapshot is not ready; retry later or trigger rebuild";

    private final GraphSnapshotReader graphSnapshotReader;
    private final GraphSnapshotRestorer graphSnapshotRestorer;
    private final GraphPathSearchService graphPathSearchService;

    public GraphPathMcpToolExecutor(GraphSnapshotReader graphSnapshotReader,
                                    GraphSnapshotRestorer graphSnapshotRestorer,
                                    GraphPathSearchService graphPathSearchService) {
        this.graphSnapshotReader = graphSnapshotReader;
        this.graphSnapshotRestorer = graphSnapshotRestorer;
        this.graphPathSearchService = graphPathSearchService;
    }

    /**
     * Describes the public contract for {@code graph.path.search}.
     *
     * @return tool descriptor including snapshot-aware request and path-search result schemas
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "Graph Path Search",
                "Reads a graph snapshot from Redis and executes path search in the MCP server process.",
                Map.of(
                        "type", "object",
                        "required", List.of("origin", "destination"),
                        "properties", Map.of(
                                "graphKey", Map.of("type", "string", "default", GraphSnapshotRedisKeys.DEFAULT_GRAPH_KEY),
                                "origin", Map.of("type", "string"),
                                "destination", Map.of("type", "string"),
                                "maxBudget", Map.of("type", "number", "default", 999999),
                                "stopoverDays", Map.of("type", "integer", "default", 0),
                                "maxSegments", Map.of("type", "integer", "default", 3),
                                "topK", Map.of("type", "integer", "default", 5)
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "graphKey", Map.of("type", "string"),
                                "snapshotVersion", Map.of("type", "string"),
                                "schemaVersion", Map.of("type", "string"),
                                "status", Map.of("type", "string"),
                                "retryable", Map.of("type", "boolean"),
                                "suggestedAction", Map.of("type", "string"),
                                "paths", Map.of("type", "array")
                        )));
    }

    /**
     * Executes a graph path search against the latest available graph snapshot.
     *
     * @param request MCP tool request containing graph key and path-search constraints
     * @return structured result that preserves business-level states such as {@code SNAPSHOT_MISS},
     *     {@code INVALID_REQUEST}, {@code NO_PATH_FOUND} or {@code SUCCESS}
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            GraphPathSearchRequest searchRequest = parseRequest(request.arguments());
            Optional<GraphSnapshot> snapshot = graphSnapshotReader.loadLatest(searchRequest.graphKey());
            if (snapshot.isEmpty()) {
                return snapshotMissResult(searchRequest.graphKey());
            }

            GraphSnapshot graphSnapshot = snapshot.get();
            // The MCP server only consumes the published read model. It must never rebuild or backfill the
            // graph from source tables when the snapshot is absent.
            RestoredFlightGraph restoredGraph = graphSnapshotRestorer.restore(graphSnapshot);
            if (!restoredGraph.hasNode(searchRequest.origin()) || !restoredGraph.hasNode(searchRequest.destination())) {
                return invalidRequestResult("origin or destination is not present in the current graph snapshot");
            }

            List<RestoredCandidatePath> paths = graphPathSearchService.search(restoredGraph, searchRequest);
            if (paths.isEmpty()) {
                return new McpToolCallResult(
                        TOOL_ID,
                        true,
                        "no path candidates found for the current constraints",
                        successBody(graphSnapshot, searchRequest.graphKey(), "NO_PATH_FOUND", List.of(), false, "NONE"),
                        null);
            }

            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    "path search completed",
                    successBody(graphSnapshot, searchRequest.graphKey(), "SUCCESS", paths, false, "NONE"),
                    null);
        } catch (IllegalArgumentException ex) {
            return invalidRequestResult(ex.getMessage());
        } catch (Exception ex) {
            return new McpToolCallResult(
                    TOOL_ID,
                    false,
                    null,
                    Map.of(
                            "status", "EXECUTION_ERROR",
                            "retryable", false,
                            "suggestedAction", "NONE",
                            "paths", List.of()
                    ),
                    ex.getMessage());
        }
    }

    private GraphPathSearchRequest parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String graphKey = stringValue(safeArguments.get("graphKey"), GraphSnapshotRedisKeys.DEFAULT_GRAPH_KEY);
        String origin = stringValue(safeArguments.get("origin"), null);
        String destination = stringValue(safeArguments.get("destination"), null);
        double maxBudget = doubleValue(safeArguments.get("maxBudget"), 999999D);
        int stopoverDays = intValue(safeArguments.get("stopoverDays"), 0);
        int maxSegments = intValue(safeArguments.get("maxSegments"), 3);
        int topK = intValue(safeArguments.get("topK"), 5);

        if (origin == null || destination == null) {
            throw new IllegalArgumentException("origin and destination are required IATA codes");
        }
        // MCP callers pass structured arguments, so validation happens here instead of letting the search
        // layer guess from raw text.
        validate(origin, destination, maxBudget, stopoverDays, maxSegments, topK);
        return new GraphPathSearchRequest(graphKey, origin, destination, maxBudget, stopoverDays, maxSegments, topK);
    }

    private void validate(String origin,
                          String destination,
                          double maxBudget,
                          int stopoverDays,
                          int maxSegments,
                          int topK) {
        if (!origin.matches("^[A-Z]{3}$") || !destination.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("origin and destination must be 3-letter uppercase IATA codes");
        }
        if (maxBudget <= 0) {
            throw new IllegalArgumentException("maxBudget must be greater than 0");
        }
        if (stopoverDays < 0) {
            throw new IllegalArgumentException("stopoverDays cannot be negative");
        }
        if (maxSegments < 1 || maxSegments > 5) {
            throw new IllegalArgumentException("maxSegments must be between 1 and 5");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
    }

    private McpToolCallResult snapshotMissResult(String graphKey) {
        return new McpToolCallResult(
                TOOL_ID,
                false,
                SNAPSHOT_MISS_MESSAGE,
                Map.of(
                        "graphKey", graphKey,
                        "status", "SNAPSHOT_MISS",
                        "message", SNAPSHOT_MISS_MESSAGE,
                        "retryable", true,
                        "suggestedAction", "RETRY_LATER",
                        "paths", List.of()
                ),
                null);
    }

    private McpToolCallResult invalidRequestResult(String message) {
        return new McpToolCallResult(
                TOOL_ID,
                false,
                null,
                Map.of(
                        "status", "INVALID_REQUEST",
                        "retryable", false,
                        "suggestedAction", "FIX_REQUEST",
                        "paths", List.of()
                ),
                message);
    }

    private Map<String, Object> successBody(GraphSnapshot graphSnapshot,
                                            String graphKey,
                                            String status,
                                            List<RestoredCandidatePath> paths,
                                            boolean retryable,
                                            String suggestedAction) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("graphKey", graphKey);
        body.put("snapshotVersion", graphSnapshot.snapshotVersion());
        body.put("schemaVersion", graphSnapshot.schemaVersion());
        body.put("status", status);
        body.put("retryable", retryable);
        body.put("suggestedAction", suggestedAction);
        body.put("pathCount", paths.size());
        body.put("paths", paths.stream().map(this::toPathMap).toList());
        return Map.copyOf(body);
    }

    private Map<String, Object> toPathMap(RestoredCandidatePath path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("segmentCount", path.segmentCount());
        body.put("transferCount", path.transferCount());
        body.put("totalPriceCny", path.totalPriceCny());
        body.put("totalDurationMinutes", path.totalDurationMinutes());
        body.put("totalDistanceKm", path.totalDistanceKm());
        body.put("averageOnTimeRate", path.averageOnTimeRate());
        body.put("hubAirports", path.hubAirports());
        body.put("legs", path.legs().stream().map(leg -> Map.of(
                "origin", leg.origin(),
                "destination", leg.destination(),
                "carrierCode", leg.carrierCode(),
                "carrierName", leg.carrierName(),
                "carrierType", leg.carrierType(),
                "priceCny", leg.priceCny(),
                "durationMinutes", leg.durationMinutes(),
                "distanceKm", leg.distanceKm()
        )).toList());
        return Map.copyOf(body);
    }

    private String stringValue(Object rawValue, String defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private double doubleValue(Object rawValue, double defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(rawValue));
    }

    private int intValue(Object rawValue, int defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(rawValue));
    }
}

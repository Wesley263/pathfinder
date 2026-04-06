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
 * {@code graph.path.search} 的服务端执行器。
 *
 * <p>该执行器是 MCP 协议调用与图快照搜索流水线之间的桥接层。
 * 之所以保留在 {@code pathfinder-mcp-server}，是因为路径搜索必须在服务进程内基于 Redis 读模型执行，
 * 不能复用 bootstrap 侧图构建流程。
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
     * 描述 {@code graph.path.search} 对外公开的工具契约。
     *
     * @return 工具描述，包含快照感知请求与路径搜索结果 schema
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
     * 基于最新可用图快照执行路径搜索。
     *
     * @param request MCP 工具请求，包含 graphKey 与路径搜索约束
     * @return 结构化结果，保留 {@code SNAPSHOT_MISS}、{@code INVALID_REQUEST}、
     *     {@code NO_PATH_FOUND}、{@code SUCCESS} 等业务状态
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
            // 服务端 MCP 只消费已发布读模型。
            // 当快照缺失时，绝不能回源源表重建或补图。
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
        // 调用方传入的是结构化 MCP 参数，因此在执行器层完成校验，
        // 避免搜索层从原始文本中“猜测”输入语义。
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

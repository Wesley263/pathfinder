package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.flightsearch.FlightSearchOption;
import com.flightpathfinder.mcp.flightsearch.FlightSearchQuery;
import com.flightpathfinder.mcp.flightsearch.FlightSearchService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code flight.search} 的服务端执行器。
 *
 * <p>该工具通过 MCP 暴露直飞检索契约，同时把真实数据源访问保留在
 * {@code pathfinder-mcp-server} 内。bootstrap 侧仅消费工具契约，不共享底层检索实现。
 */
@Component
public class FlightSearchMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "flight.search";

    private final FlightSearchService flightSearchService;

    public FlightSearchMcpToolExecutor(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    /**
     * 描述直飞查询的 MCP 工具契约。
     *
     * @return {@code flight.search} 的请求参数与结果 schema 描述
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "Flight Search",
                "Searches direct flight options by airport pair and date window using the MCP server's own database access.",
                Map.of(
                        "type", "object",
                        "required", List.of("origin", "destination", "date"),
                        "properties", Map.of(
                                "origin", Map.of("type", "string"),
                                "destination", Map.of("type", "string"),
                                "date", Map.of("type", "string"),
                                "flexibilityDays", Map.of("type", "integer", "default", 0),
                                "topK", Map.of("type", "integer", "default", 5)
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "origin", Map.of("type", "string"),
                                "destination", Map.of("type", "string"),
                                "date", Map.of("type", "string"),
                                "flexibilityDays", Map.of("type", "integer"),
                                "topK", Map.of("type", "integer"),
                                "status", Map.of("type", "string"),
                                "message", Map.of("type", "string"),
                                "flightCount", Map.of("type", "integer"),
                                "flights", Map.of("type", "array")
                        )));
    }

    /**
     * 基于 MCP 服务端数据源执行直飞检索。
     *
     * @param request MCP 请求，包含结构化机场/日期约束
     * @return 结构化检索结果，状态可能为 {@code SUCCESS}、{@code NO_FLIGHTS_FOUND}，
     *     或 {@code INVALID_REQUEST}/{@code EXECUTION_ERROR}
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            FlightSearchQuery query = parseRequest(request.arguments());
            List<FlightSearchOption> options = flightSearchService.search(query);
            if (options.isEmpty()) {
                return new McpToolCallResult(
                        TOOL_ID,
                        true,
                        "no flight options found for the current query window",
                        successBody(query, "NO_FLIGHTS_FOUND", "no direct flight options found for the current query window", List.of()),
                        null);
            }

            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    "flight search completed",
                    successBody(query, "SUCCESS", "flight search completed", options),
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
                            "flightCount", 0,
                            "flights", List.of()),
                    ex.getMessage());
        }
    }

    private FlightSearchQuery parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String origin = stringValue(safeArguments.get("origin"));
        String destination = stringValue(safeArguments.get("destination"));
        String date = stringValue(safeArguments.get("date"));
        int flexibilityDays = intValue(safeArguments.get("flexibilityDays"), 0);
        int topK = intValue(safeArguments.get("topK"), 5);

        if (origin == null || destination == null || date == null) {
            throw new IllegalArgumentException("origin, destination and date are required");
        }
        if (!origin.matches("^[A-Z]{3}$") || !destination.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("origin and destination must be 3-letter uppercase IATA codes");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("origin and destination cannot be the same");
        }
        if (flexibilityDays < 0 || flexibilityDays > 7) {
            throw new IllegalArgumentException("flexibilityDays must be between 0 and 7");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }

        try {
            // 执行器统一做一次协议入参归一化，让下游 JDBC 检索可直接依赖强类型查询对象。
            return new FlightSearchQuery(origin, destination, LocalDate.parse(date), flexibilityDays, topK);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("date must be in yyyy-MM-dd format");
        }
    }

    private McpToolCallResult invalidRequestResult(String message) {
        return new McpToolCallResult(
                TOOL_ID,
                false,
                null,
                Map.of(
                        "status", "INVALID_REQUEST",
                        "message", message,
                        "retryable", false,
                        "suggestedAction", "FIX_REQUEST",
                        "flightCount", 0,
                        "flights", List.of()),
                message);
    }

    private Map<String, Object> successBody(FlightSearchQuery query,
                                            String status,
                                            String message,
                                            List<FlightSearchOption> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", query.origin());
        body.put("destination", query.destination());
        body.put("date", query.date().toString());
        body.put("flexibilityDays", query.flexibilityDays());
        body.put("topK", query.topK());
        body.put("status", status);
        body.put("message", message);
        body.put("retryable", false);
        body.put("suggestedAction", "NONE");
        body.put("flightCount", options.size());
        body.put("flights", options.stream().map(this::toFlightMap).toList());
        return Map.copyOf(body);
    }

    private Map<String, Object> toFlightMap(FlightSearchOption option) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("airlineCode", option.airlineCode());
        body.put("airlineName", option.airlineName());
        body.put("airlineType", option.airlineType());
        body.put("origin", option.origin());
        body.put("destination", option.destination());
        body.put("date", option.date());
        body.put("priceCny", option.priceCny());
        body.put("basePriceCny", option.basePriceCny());
        body.put("durationMinutes", option.durationMinutes());
        body.put("distanceKm", option.distanceKm());
        body.put("lowCostCarrier", option.lowCostCarrier());
        return Map.copyOf(body);
    }

    private String stringValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
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

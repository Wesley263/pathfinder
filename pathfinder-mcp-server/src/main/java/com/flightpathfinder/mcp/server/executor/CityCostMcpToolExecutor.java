package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.citycost.CityCostItem;
import com.flightpathfinder.mcp.citycost.CityCostQuery;
import com.flightpathfinder.mcp.citycost.CityCostResult;
import com.flightpathfinder.mcp.citycost.CityCostService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Server-side executor for {@code city.cost}.
 *
 * <p>This tool exposes city-cost lookup as a standalone MCP capability backed by the MCP server's own
 * dataset access. It does not call back into bootstrap because cost data ownership is local to the tool.
 */
@Component
public class CityCostMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "city.cost";
    private static final Pattern IATA_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final CityCostService cityCostService;

    public CityCostMcpToolExecutor(CityCostService cityCostService) {
        this.cityCostService = cityCostService;
    }

    /**
     * Describes the MCP contract for city cost lookup.
     *
     * @return descriptor for {@code city.cost}
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "City Cost",
                "Looks up daily living cost data for one or more cities using the MCP server's city cost dataset.",
                Map.of(
                        "type", "object",
                        "required", List.of("iataCodes"),
                        "properties", Map.of(
                                "iataCodes", Map.of("type", "string")
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of("type", "string"),
                                "message", Map.of("type", "string"),
                                "retryable", Map.of("type", "boolean"),
                                "suggestedAction", Map.of("type", "string"),
                                "requestedCities", Map.of("type", "array"),
                                "requestedCityCount", Map.of("type", "integer"),
                                "matchedCityCount", Map.of("type", "integer"),
                                "missingCities", Map.of("type", "array"),
                                "costs", Map.of("type", "array")
                        )));
    }

    /**
     * Executes a city-cost lookup for one or more normalized city codes.
     *
     * @param request MCP request containing structured city or airport codes
     * @return structured result with {@code SUCCESS}, {@code PARTIAL_SUCCESS}, {@code DATA_NOT_FOUND},
     *     or request/execution errors
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            CityCostQuery query = parseRequest(request.arguments());
            CityCostResult result = cityCostService.lookup(query);
            String status = resolveStatus(result);
            String message = buildMessage(status);
            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    message,
                    successBody(result, status, message),
                    null);
        } catch (IllegalArgumentException exception) {
            return invalidRequestResult(exception.getMessage());
        } catch (Exception exception) {
            return new McpToolCallResult(
                    TOOL_ID,
                    false,
                    null,
                    Map.of(
                            "status", "EXECUTION_ERROR",
                            "message", "city cost lookup failed unexpectedly",
                            "retryable", false,
                            "suggestedAction", "NONE",
                            "matchedCityCount", 0,
                            "missingCities", List.of(),
                            "costs", List.of()),
                    exception.getMessage());
        }
    }

    private CityCostQuery parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String iataCodes = stringValue(safeArguments.get("iataCodes"));
        if (iataCodes == null || iataCodes.isBlank()) {
            throw new IllegalArgumentException("iataCodes is required");
        }
        List<String> codes = parseIataCodes(iataCodes);
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("iataCodes must contain at least one 3-letter city or airport code");
        }
        return new CityCostQuery(codes);
    }

    private List<String> parseIataCodes(String iataCodes) {
        List<String> codes = new ArrayList<>();
        for (String rawCode : iataCodes.split("[,;\\s]+")) {
            String normalized = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
            if (!IATA_CODE_PATTERN.matcher(normalized).matches()) {
                continue;
            }
            // Canonicalizing the request here keeps datasource lookup deterministic and avoids duplicate rows
            // in the management-facing result.
            if (!codes.contains(normalized)) {
                codes.add(normalized);
            }
        }
        return List.copyOf(codes);
    }

    private String resolveStatus(CityCostResult result) {
        if (result.items().isEmpty()) {
            return "DATA_NOT_FOUND";
        }
        if (!result.missingCities().isEmpty()) {
            return "PARTIAL_SUCCESS";
        }
        return "SUCCESS";
    }

    private String buildMessage(String status) {
        return switch (status) {
            case "DATA_NOT_FOUND" -> "city cost data is not available for the requested cities";
            case "PARTIAL_SUCCESS" -> "city cost lookup completed with partial coverage";
            default -> "city cost lookup completed";
        };
    }

    private Map<String, Object> successBody(CityCostResult result, String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("message", message);
        body.put("retryable", false);
        body.put("suggestedAction", "NONE");
        body.put("requestedCities", result.requestedCities());
        body.put("requestedCityCount", result.requestedCities().size());
        body.put("matchedCityCount", result.items().size());
        body.put("missingCities", result.missingCities());
        body.put("costs", result.items().stream().map(this::toCostMap).toList());
        return Map.copyOf(body);
    }

    private Map<String, Object> toCostMap(CityCostItem item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("iataCode", item.iataCode());
        body.put("city", item.city());
        body.put("country", item.country());
        body.put("countryCode", item.countryCode());
        body.put("dailyCostUsd", item.dailyCostUsd());
        body.put("accommodationUsd", item.accommodationUsd());
        body.put("mealCostUsd", item.mealCostUsd());
        body.put("transportationUsd", item.transportationUsd());
        return Map.copyOf(body);
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
                        "matchedCityCount", 0,
                        "missingCities", List.of(),
                        "costs", List.of()),
                message);
    }

    private String stringValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
    }
}

package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.pricelookup.PriceLookupCityPair;
import com.flightpathfinder.mcp.pricelookup.PriceLookupItem;
import com.flightpathfinder.mcp.pricelookup.PriceLookupQuery;
import com.flightpathfinder.mcp.pricelookup.PriceLookupResult;
import com.flightpathfinder.mcp.pricelookup.PriceLookupService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Server-side executor for {@code price.lookup}.
 *
 * <p>This tool presents price comparison as its own MCP contract even though the server-side implementation
 * reuses direct-flight search internally. Keeping it as a separate executor lets the MCP surface expose its
 * own input syntax and partial-coverage semantics.
 */
@Component
public class PriceLookupMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "price.lookup";
    private static final Pattern CITY_PAIR_PATTERN = Pattern.compile("^[A-Z]{3},[A-Z]{3}$");

    private final PriceLookupService priceLookupService;

    public PriceLookupMcpToolExecutor(PriceLookupService priceLookupService) {
        this.priceLookupService = priceLookupService;
    }

    /**
     * Describes the MCP contract for lowest-price comparison across city pairs.
     *
     * @return descriptor for {@code price.lookup}
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "Price Lookup",
                "Compares lowest prices across multiple city pairs on the same date by reusing the server-side flight search capability.",
                Map.of(
                        "type", "object",
                        "required", List.of("cityPairs", "date"),
                        "properties", Map.of(
                                "cityPairs", Map.of("type", "string"),
                                "date", Map.of("type", "string")
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "date", Map.of("type", "string"),
                                "status", Map.of("type", "string"),
                                "message", Map.of("type", "string"),
                                "retryable", Map.of("type", "boolean"),
                                "suggestedAction", Map.of("type", "string"),
                                "requestedPairs", Map.of("type", "array"),
                                "requestedPairCount", Map.of("type", "integer"),
                                "matchedPairCount", Map.of("type", "integer"),
                                "missingPairs", Map.of("type", "array"),
                                "prices", Map.of("type", "array")
                        )));
    }

    /**
     * Executes a multi-pair price lookup.
     *
     * @param request MCP request whose arguments must already be structured rather than free text
     * @return structured result preserving {@code SUCCESS}, {@code PARTIAL_SUCCESS},
     *     {@code NO_PRICE_FOUND}, or request/execution errors
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            PriceLookupQuery query = parseRequest(request.arguments());
            PriceLookupResult result = priceLookupService.lookup(query);
            if (result.items().isEmpty()) {
                return new McpToolCallResult(
                        TOOL_ID,
                        true,
                        "no price entries found for the requested city pairs",
                        successBody(result, "NO_PRICE_FOUND", "no price entries found for the requested city pairs"),
                        null);
            }
            if (!result.missingPairs().isEmpty()) {
                return new McpToolCallResult(
                        TOOL_ID,
                        true,
                        "price lookup completed with partial coverage",
                        successBody(result, "PARTIAL_SUCCESS", "price lookup completed with partial coverage"),
                        null);
            }
            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    "price lookup completed",
                    successBody(result, "SUCCESS", "price lookup completed"),
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
                            "message", "price lookup failed unexpectedly",
                            "retryable", false,
                            "suggestedAction", "NONE",
                            "matchedPairCount", 0,
                            "missingPairs", List.of(),
                            "prices", List.of()),
                    ex.getMessage());
        }
    }

    private PriceLookupQuery parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String cityPairs = stringValue(safeArguments.get("cityPairs"));
        String date = stringValue(safeArguments.get("date"));
        if (cityPairs == null || cityPairs.isBlank() || date == null || date.isBlank()) {
            throw new IllegalArgumentException("cityPairs and date are required");
        }

        LocalDate queryDate;
        try {
            queryDate = LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("date must be in yyyy-MM-dd format");
        }

        List<PriceLookupCityPair> pairs = parseCityPairs(cityPairs);
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("cityPairs must use ORIGIN,DESTINATION;ORIGIN,DESTINATION format");
        }

        return new PriceLookupQuery(queryDate, pairs);
    }

    private List<PriceLookupCityPair> parseCityPairs(String cityPairs) {
        List<PriceLookupCityPair> pairs = new ArrayList<>();
        for (String rawPair : cityPairs.split(";")) {
            String candidate = rawPair == null ? "" : rawPair.trim().toUpperCase(Locale.ROOT);
            if (!CITY_PAIR_PATTERN.matcher(candidate).matches()) {
                continue;
            }
            String[] tokens = candidate.split(",");
            if (tokens.length != 2 || tokens[0].equals(tokens[1])) {
                continue;
            }
            PriceLookupCityPair cityPair = new PriceLookupCityPair(tokens[0], tokens[1]);
            // Deduping at the protocol edge keeps the downstream comparison result stable and easy to audit.
            if (!pairs.contains(cityPair)) {
                pairs.add(cityPair);
            }
        }
        return List.copyOf(pairs);
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
                        "matchedPairCount", 0,
                        "missingPairs", List.of(),
                        "prices", List.of()),
                message);
    }

    private Map<String, Object> successBody(PriceLookupResult result, String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", result.date());
        body.put("status", status);
        body.put("message", message);
        body.put("retryable", false);
        body.put("suggestedAction", "NONE");
        body.put("requestedPairs", result.requestedPairs());
        body.put("requestedPairCount", result.requestedPairs().size());
        body.put("matchedPairCount", result.items().size());
        body.put("missingPairs", result.missingPairs());
        body.put("prices", result.items().stream().map(this::toPriceMap).toList());
        return Map.copyOf(body);
    }

    private Map<String, Object> toPriceMap(PriceLookupItem item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cityPair", item.cityPair());
        body.put("origin", item.origin());
        body.put("destination", item.destination());
        body.put("airlineCode", item.airlineCode());
        body.put("airlineName", item.airlineName());
        body.put("airlineType", item.airlineType());
        body.put("date", item.date());
        body.put("lowestPriceCny", item.lowestPriceCny());
        body.put("basePriceCny", item.basePriceCny());
        body.put("durationMinutes", item.durationMinutes());
        body.put("distanceKm", item.distanceKm());
        body.put("lowCostCarrier", item.lowCostCarrier());
        return Map.copyOf(body);
    }

    private String stringValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
    }
}

package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminAirportLookupResult;
import com.flightpathfinder.admin.service.AdminAirportSummary;
import com.flightpathfinder.admin.service.AdminDiagnosticsService;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticOption;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticResult;
import com.flightpathfinder.admin.service.AdminPathDiagnosticCandidate;
import com.flightpathfinder.admin.service.AdminPathDiagnosticLeg;
import com.flightpathfinder.admin.service.AdminPathDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminPathDiagnosticResult;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.mcp.client.McpToolDiscoveryService;
import com.flightpathfinder.rag.core.mcp.client.RemoteMcpToolExecutor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * Default admin-assisted diagnostics service.
 *
 * <p>This service keeps direct verification flows on the admin surface. Operators can run raw MCP-backed or
 * JDBC-backed checks without conflating them with user-facing search APIs.
 */
@Service
public class DefaultAdminDiagnosticsService implements AdminDiagnosticsService {

    private static final String FLIGHT_SEARCH_TOOL_ID = "flight.search";
    private static final String GRAPH_PATH_TOOL_ID = "graph.path.search";

    private static final String AIRPORT_LOOKUP_SQL = """
            SELECT iata_code, icao_code, name_en, name_cn, city, city_cn, country_code,
                   latitude, longitude, timezone, type, source
            FROM t_airport
            WHERE deleted = 0
              AND iata_code = ?
            LIMIT 1
            """;

    private static final String OUTGOING_ROUTE_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_route
            WHERE deleted = 0
              AND source_airport_iata = ?
            """;

    private static final String INCOMING_ROUTE_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_route
            WHERE deleted = 0
              AND dest_airport_iata = ?
            """;

    private final LocalMcpToolRegistry localMcpToolRegistry;
    private final McpToolDiscoveryService mcpToolDiscoveryService;
    private final RemoteMcpToolExecutor remoteMcpToolExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final GraphSnapshotQueryService graphSnapshotQueryService;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public DefaultAdminDiagnosticsService(LocalMcpToolRegistry localMcpToolRegistry,
                                          McpToolDiscoveryService mcpToolDiscoveryService,
                                          RemoteMcpToolExecutor remoteMcpToolExecutor,
                                          JdbcTemplate jdbcTemplate,
                                          GraphSnapshotQueryService graphSnapshotQueryService,
                                          GraphSnapshotProperties graphSnapshotProperties) {
        this.localMcpToolRegistry = localMcpToolRegistry;
        this.mcpToolDiscoveryService = mcpToolDiscoveryService;
        this.remoteMcpToolExecutor = remoteMcpToolExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.graphSnapshotQueryService = graphSnapshotQueryService;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * Runs a direct flight diagnostic against the current MCP catalog.
     *
     * @param query structured diagnostic query
     * @return admin-facing diagnostic result
     */
    @Override
    public AdminFlightDiagnosticResult searchFlights(AdminFlightDiagnosticQuery query) {
        AdminFlightDiagnosticQuery safeQuery = query == null
                ? new AdminFlightDiagnosticQuery("", "", "", 0, 5)
                : query;
        Optional<McpToolDescriptor> descriptor = resolveToolDescriptor(FLIGHT_SEARCH_TOOL_ID);
        if (descriptor.isEmpty()) {
            return new AdminFlightDiagnosticResult(
                    FLIGHT_SEARCH_TOOL_ID,
                    false,
                    false,
                    "TOOL_UNAVAILABLE",
                    "flight.search is not available from the current MCP catalog",
                    "",
                    false,
                    "CHECK_MCP_SERVER",
                    safeQuery.origin(),
                    safeQuery.destination(),
                    safeQuery.date(),
                    safeQuery.flexibilityDays(),
                    safeQuery.topK(),
                    0,
                    Instant.now(),
                    List.of());
        }

        McpToolCallResult toolResult = remoteMcpToolExecutor.execute(new McpToolCallRequest(
                FLIGHT_SEARCH_TOOL_ID,
                Map.of(
                        "origin", safeString(safeQuery.origin()),
                        "destination", safeString(safeQuery.destination()),
                        "date", safeString(safeQuery.date()),
                        "flexibilityDays", safeQuery.flexibilityDays(),
                        "topK", safeQuery.topK())));
        Map<String, Object> content = safeStructuredContent(toolResult);
        return new AdminFlightDiagnosticResult(
                FLIGHT_SEARCH_TOOL_ID,
                true,
                toolResult != null && toolResult.success(),
                stringValue(content.get("status"), toolResult != null && toolResult.success() ? "SUCCESS" : "TOOL_ERROR"),
                firstNonBlank(stringValue(content.get("message")), toolResult == null ? "" : toolResult.content()),
                toolResult == null ? "" : safeString(toolResult.errorMessage()),
                booleanValue(content.get("retryable")),
                stringValue(content.get("suggestedAction")),
                stringValue(content.get("origin"), safeQuery.origin()),
                stringValue(content.get("destination"), safeQuery.destination()),
                stringValue(content.get("date"), safeQuery.date()),
                intValue(content.get("flexibilityDays"), safeQuery.flexibilityDays()),
                intValue(content.get("topK"), safeQuery.topK()),
                intValue(content.get("flightCount"), 0),
                Instant.now(),
                listValue(content.get("flights")).stream()
                        .map(this::toFlightOption)
                        .toList());
    }

    /**
     * Runs a direct graph-path diagnostic against the current MCP catalog.
     *
     * @param query structured diagnostic query
     * @return admin-facing path diagnostic result
     */
    @Override
    public AdminPathDiagnosticResult searchPaths(AdminPathDiagnosticQuery query) {
        AdminPathDiagnosticQuery safeQuery = query == null
                ? new AdminPathDiagnosticQuery("", "", "", 999999D, 0, 3, 5)
                : query;
        String graphKey = normalizeGraphKey(safeQuery.graphKey());
        Optional<McpToolDescriptor> descriptor = resolveToolDescriptor(GRAPH_PATH_TOOL_ID);
        if (descriptor.isEmpty()) {
            return new AdminPathDiagnosticResult(
                    GRAPH_PATH_TOOL_ID,
                    false,
                    false,
                    "TOOL_UNAVAILABLE",
                    "graph.path.search is not available from the current MCP catalog",
                    "",
                    false,
                    false,
                    "CHECK_MCP_SERVER",
                    graphKey,
                    "",
                    "",
                    safeQuery.origin(),
                    safeQuery.destination(),
                    safeQuery.maxBudget(),
                    safeQuery.stopoverDays(),
                    safeQuery.maxSegments(),
                    safeQuery.topK(),
                    0,
                    Instant.now(),
                    List.of());
        }

        McpToolCallResult toolResult = remoteMcpToolExecutor.execute(new McpToolCallRequest(
                GRAPH_PATH_TOOL_ID,
                Map.of(
                        "graphKey", graphKey,
                        "origin", safeString(safeQuery.origin()),
                        "destination", safeString(safeQuery.destination()),
                        "maxBudget", safeQuery.maxBudget(),
                        "stopoverDays", safeQuery.stopoverDays(),
                        "maxSegments", safeQuery.maxSegments(),
                        "topK", safeQuery.topK())));
        Map<String, Object> content = safeStructuredContent(toolResult);
        String status = stringValue(content.get("status"), toolResult != null && toolResult.success() ? "SUCCESS" : "TOOL_ERROR");
        return new AdminPathDiagnosticResult(
                GRAPH_PATH_TOOL_ID,
                true,
                toolResult != null && toolResult.success(),
                status,
                firstNonBlank(stringValue(content.get("message")), toolResult == null ? "" : toolResult.content()),
                toolResult == null ? "" : safeString(toolResult.errorMessage()),
                "SNAPSHOT_MISS".equals(status),
                booleanValue(content.get("retryable")),
                stringValue(content.get("suggestedAction")),
                stringValue(content.get("graphKey"), graphKey),
                stringValue(content.get("snapshotVersion")),
                stringValue(content.get("schemaVersion")),
                safeString(safeQuery.origin()),
                safeString(safeQuery.destination()),
                safeQuery.maxBudget(),
                safeQuery.stopoverDays(),
                safeQuery.maxSegments(),
                safeQuery.topK(),
                intValue(content.get("pathCount"), 0),
                Instant.now(),
                listValue(content.get("paths")).stream()
                        .map(this::toPathCandidate)
                        .toList());
    }

    /**
     * Looks up one airport directly from the imported operational dataset.
     *
     * @param iataCode airport IATA code to inspect
     * @return admin-facing airport lookup result
     */
    @Override
    public AdminAirportLookupResult lookupAirport(String iataCode) {
        String normalizedIataCode = normalizeAirportCode(iataCode);
        if (normalizedIataCode.isBlank()) {
            return new AdminAirportLookupResult(
                    "",
                    "INVALID_REQUEST",
                    "airport iata code is required",
                    Instant.now(),
                    null);
        }

        Optional<AirportRow> airportRow = jdbcTemplate.query(AIRPORT_LOOKUP_SQL, new AirportRowMapper(), normalizedIataCode)
                .stream()
                .findFirst();
        if (airportRow.isEmpty()) {
            return new AdminAirportLookupResult(
                    normalizedIataCode,
                    "NOT_FOUND",
                    "airport data is not available for iata=" + normalizedIataCode,
                    Instant.now(),
                    null);
        }

        long outgoingRouteCount = queryCount(OUTGOING_ROUTE_COUNT_SQL, normalizedIataCode);
        long incomingRouteCount = queryCount(INCOMING_ROUTE_COUNT_SQL, normalizedIataCode);
        Optional<GraphSnapshot> currentSnapshot = graphSnapshotQueryService.loadCurrent(graphSnapshotProperties.getDefaultGraphKey());
        boolean presentInCurrentSnapshot = currentSnapshot.stream()
                .flatMap(snapshot -> snapshot.nodes().stream())
                .anyMatch(node -> normalizedIataCode.equals(node.airportCode()));
        // Diagnostics show snapshot presence because operators often need to understand whether an airport is
        // present in source data only or also in the currently published graph read model.
        String currentSnapshotStatus = currentSnapshot.isPresent() ? "READY" : "SNAPSHOT_MISS";
        String currentSnapshotVersion = currentSnapshot.map(GraphSnapshot::snapshotVersion).orElse("");
        Instant observedAt = Instant.now();

        AirportRow row = airportRow.get();
        return new AdminAirportLookupResult(
                normalizedIataCode,
                "FOUND",
                "airport lookup completed",
                observedAt,
                new AdminAirportSummary(
                        row.iataCode(),
                        row.icaoCode(),
                        row.nameEn(),
                        row.nameCn(),
                        row.city(),
                        row.cityCn(),
                        row.countryCode(),
                        row.latitude(),
                        row.longitude(),
                        row.timezone(),
                        row.type(),
                        row.source(),
                        outgoingRouteCount,
                        incomingRouteCount,
                        outgoingRouteCount + incomingRouteCount,
                        currentSnapshotStatus,
                        presentInCurrentSnapshot,
                        currentSnapshotVersion,
                        observedAt));
    }

    private Optional<McpToolDescriptor> resolveToolDescriptor(String toolId) {
        Optional<McpToolDescriptor> localDescriptor = localMcpToolRegistry.findByToolId(toolId);
        if (localDescriptor.isPresent()) {
            return localDescriptor;
        }
        // Diagnostics prefer a fresh catalog view, but they still fall back to the local registry so operators
        // can inspect the last-known state even if discovery is temporarily unavailable.
        List<McpToolDescriptor> refreshedTools = mcpToolDiscoveryService.refreshToolCatalog();
        return localMcpToolRegistry.findByToolId(toolId)
                .or(() -> refreshedTools.stream().filter(tool -> toolId.equals(tool.toolId())).findFirst());
    }

    private AdminFlightDiagnosticOption toFlightOption(Map<String, Object> rawFlight) {
        return new AdminFlightDiagnosticOption(
                stringValue(rawFlight.get("airlineCode")),
                stringValue(rawFlight.get("airlineName")),
                stringValue(rawFlight.get("airlineType")),
                stringValue(rawFlight.get("origin")),
                stringValue(rawFlight.get("destination")),
                stringValue(rawFlight.get("date")),
                doubleValue(rawFlight.get("priceCny"), 0D),
                doubleValue(rawFlight.get("basePriceCny"), 0D),
                intValue(rawFlight.get("durationMinutes"), 0),
                doubleValue(rawFlight.get("distanceKm"), 0D),
                booleanValue(rawFlight.get("lowCostCarrier")));
    }

    private AdminPathDiagnosticCandidate toPathCandidate(Map<String, Object> rawPath) {
        return new AdminPathDiagnosticCandidate(
                intValue(rawPath.get("segmentCount"), 0),
                intValue(rawPath.get("transferCount"), 0),
                doubleValue(rawPath.get("totalPriceCny"), 0D),
                intValue(rawPath.get("totalDurationMinutes"), 0),
                doubleValue(rawPath.get("totalDistanceKm"), 0D),
                doubleValue(rawPath.get("averageOnTimeRate"), 0D),
                stringListValue(rawPath.get("hubAirports")),
                listValue(rawPath.get("legs")).stream()
                        .map(this::toPathLeg)
                        .toList());
    }

    private AdminPathDiagnosticLeg toPathLeg(Map<String, Object> rawLeg) {
        return new AdminPathDiagnosticLeg(
                stringValue(rawLeg.get("origin")),
                stringValue(rawLeg.get("destination")),
                stringValue(rawLeg.get("carrierCode")),
                stringValue(rawLeg.get("carrierName")),
                stringValue(rawLeg.get("carrierType")),
                doubleValue(rawLeg.get("priceCny"), 0D),
                intValue(rawLeg.get("durationMinutes"), 0),
                doubleValue(rawLeg.get("distanceKm"), 0D));
    }

    private Map<String, Object> safeStructuredContent(McpToolCallResult toolResult) {
        return toolResult == null || toolResult.structuredContent() == null
                ? Map.of()
                : toolResult.structuredContent();
    }

    private List<Map<String, Object>> listValue(Object rawValue) {
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::copyMap)
                    .toList();
        }
        return List.of();
    }

    private List<String> stringListValue(Object rawValue) {
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(value -> value == null ? "" : String.valueOf(value).trim())
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copyMap(Map<?, ?> rawMap) {
        return (Map<String, Object>) rawMap;
    }

    private String stringValue(Object rawValue) {
        return stringValue(rawValue, "");
    }

    private String stringValue(Object rawValue, String defaultValue) {
        if (rawValue == null) {
            return defaultValue == null ? "" : defaultValue;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? (defaultValue == null ? "" : defaultValue) : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private int intValue(Object rawValue, int defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(rawValue).trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private double doubleValue(Object rawValue, double defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(rawValue).trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private boolean booleanValue(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (rawValue instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue.trim());
        }
        return false;
    }

    private long queryCount(String sql, String iataCode) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, iataCode);
        return count == null ? 0L : count;
    }

    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey.trim();
    }

    private String normalizeAirportCode(String value) {
        return safeString(value).toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static final class AirportRowMapper implements RowMapper<AirportRow> {

        @Override
        public AirportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AirportRow(
                    rs.getString("iata_code"),
                    rs.getString("icao_code"),
                    rs.getString("name_en"),
                    rs.getString("name_cn"),
                    rs.getString("city"),
                    rs.getString("city_cn"),
                    rs.getString("country_code"),
                    getDouble(rs, "latitude"),
                    getDouble(rs, "longitude"),
                    rs.getString("timezone"),
                    rs.getString("type"),
                    rs.getString("source"));
        }

        private static double getDouble(ResultSet resultSet, String column) throws SQLException {
            double value = resultSet.getDouble(column);
            return resultSet.wasNull() ? 0.0D : value;
        }
    }

    private record AirportRow(
            String iataCode,
            String icaoCode,
            String nameEn,
            String nameCn,
            String city,
            String cityCn,
            String countryCode,
            double latitude,
            double longitude,
            String timezone,
            String type,
            String source) {
    }
}

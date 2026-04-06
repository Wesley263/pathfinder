package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminAirportLookupResult;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticResult;
import com.flightpathfinder.admin.service.AdminPathDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminPathDiagnosticResult;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotNode;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.mcp.client.McpToolDiscoveryService;
import com.flightpathfinder.rag.core.mcp.client.RemoteMcpToolExecutor;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultAdminDiagnosticsServiceTest {

    @Mock
    private LocalMcpToolRegistry localMcpToolRegistry;

    @Mock
    private McpToolDiscoveryService mcpToolDiscoveryService;

    @Mock
    private RemoteMcpToolExecutor remoteMcpToolExecutor;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private GraphSnapshotQueryService graphSnapshotQueryService;

    private DefaultAdminDiagnosticsService diagnosticsService;

    @BeforeEach
    void setUp() {
        GraphSnapshotProperties graphSnapshotProperties = new GraphSnapshotProperties();
        graphSnapshotProperties.setDefaultGraphKey("flight-main");
        diagnosticsService = new DefaultAdminDiagnosticsService(
                localMcpToolRegistry,
                mcpToolDiscoveryService,
                remoteMcpToolExecutor,
                jdbcTemplate,
                graphSnapshotQueryService,
                graphSnapshotProperties);
    }

    @Test
    void searchFlights_shouldReturnToolUnavailableWhenCatalogMissing() {
        when(localMcpToolRegistry.findByToolId("flight.search")).thenReturn(Optional.empty(), Optional.empty());
        when(mcpToolDiscoveryService.refreshToolCatalog()).thenReturn(List.of());

        AdminFlightDiagnosticResult result = diagnosticsService.searchFlights(null);

        assertFalse(result.toolAvailable());
        assertFalse(result.success());
        assertEquals("TOOL_UNAVAILABLE", result.status());
        assertEquals("CHECK_MCP_SERVER", result.suggestedAction());
        assertEquals(5, result.topK());
        verify(remoteMcpToolExecutor, never()).execute(any());
    }

    @Test
    void searchFlights_shouldMapStructuredToolResultAndRequestArguments() {
        when(localMcpToolRegistry.findByToolId("flight.search")).thenReturn(Optional.of(descriptor("flight.search")));
        when(remoteMcpToolExecutor.execute(any())).thenReturn(new McpToolCallResult(
                "flight.search",
                true,
                "fallback",
            Map.ofEntries(
                Map.entry("status", "SUCCESS"),
                Map.entry("message", "flight options loaded"),
                Map.entry("origin", "PVG"),
                Map.entry("destination", "LHR"),
                Map.entry("date", "2026-05-01"),
                Map.entry("flexibilityDays", 2),
                Map.entry("topK", 3),
                Map.entry("flightCount", 1),
                Map.entry("retryable", false),
                Map.entry("suggestedAction", "NONE"),
                Map.entry("flights", List.of(Map.ofEntries(
                            Map.entry("airlineCode", "MU"),
                            Map.entry("airlineName", "China Eastern"),
                            Map.entry("airlineType", "FULL_SERVICE"),
                            Map.entry("origin", "PVG"),
                            Map.entry("destination", "LHR"),
                            Map.entry("date", "2026-05-01"),
                            Map.entry("priceCny", 5200.0),
                            Map.entry("basePriceCny", 5000.0),
                            Map.entry("durationMinutes", 720),
                            Map.entry("distanceKm", 9200.0),
                    Map.entry("lowCostCarrier", false))))),
                ""));

        AdminFlightDiagnosticResult result = diagnosticsService.searchFlights(
                new AdminFlightDiagnosticQuery(" PVG ", " LHR ", "2026-05-01", 2, 3));

        assertTrue(result.toolAvailable());
        assertTrue(result.success());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.flightCount());
        assertEquals("MU", result.flights().get(0).airlineCode());
        assertEquals(5200.0, result.flights().get(0).priceCny());

        ArgumentCaptor<McpToolCallRequest> requestCaptor = ArgumentCaptor.forClass(McpToolCallRequest.class);
        verify(remoteMcpToolExecutor).execute(requestCaptor.capture());
        McpToolCallRequest request = requestCaptor.getValue();
        assertEquals("flight.search", request.toolId());
        assertEquals("PVG", request.arguments().get("origin"));
        assertEquals("LHR", request.arguments().get("destination"));
        assertEquals(3, request.arguments().get("topK"));
    }

    @Test
    void searchPaths_shouldMarkSnapshotMissWhenToolReturnsSnapshotMissStatus() {
        when(localMcpToolRegistry.findByToolId("graph.path.search")).thenReturn(Optional.of(descriptor("graph.path.search")));
        when(remoteMcpToolExecutor.execute(any())).thenReturn(new McpToolCallResult(
                "graph.path.search",
                true,
                "",
                Map.of(
                        "status", "SNAPSHOT_MISS",
                        "message", "snapshot missing",
                        "graphKey", "flight-main",
                        "pathCount", 0,
                        "paths", List.of()),
                ""));

        AdminPathDiagnosticResult result = diagnosticsService.searchPaths(
                new AdminPathDiagnosticQuery("", "PVG", "LHR", 9999.0, 0, 3, 5));

        assertTrue(result.toolAvailable());
        assertTrue(result.success());
        assertEquals("SNAPSHOT_MISS", result.status());
        assertTrue(result.snapshotMiss());
        assertEquals("flight-main", result.graphKey());
    }

    @Test
    void lookupAirport_shouldReturnInvalidRequestWhenIataBlank() {
        AdminAirportLookupResult result = diagnosticsService.lookupAirport("  ");

        assertEquals("INVALID_REQUEST", result.status());
        assertNull(result.airport());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), anyString());
    }

    @Test
    void lookupAirport_shouldReturnFoundAirportAndSnapshotPresence() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("PVG"))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<Object> rowMapper = (RowMapper<Object>) invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getString("iata_code")).thenReturn("PVG");
            when(resultSet.getString("icao_code")).thenReturn("ZSPD");
            when(resultSet.getString("name_en")).thenReturn("Pudong");
            when(resultSet.getString("name_cn")).thenReturn("");
            when(resultSet.getString("city")).thenReturn("Shanghai");
            when(resultSet.getString("city_cn")).thenReturn("");
            when(resultSet.getString("country_code")).thenReturn("CN");
            when(resultSet.getDouble("latitude")).thenReturn(31.14);
            when(resultSet.getDouble("longitude")).thenReturn(121.81);
            when(resultSet.wasNull()).thenReturn(false, false);
            when(resultSet.getString("timezone")).thenReturn("Asia/Shanghai");
            when(resultSet.getString("type")).thenReturn("airport");
            when(resultSet.getString("source")).thenReturn("openflights");
            return List.of(rowMapper.mapRow(resultSet, 0));
        });
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("PVG"))).thenReturn(3L, 4L);
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.of(snapshotWithPvgNode()));

        AdminAirportLookupResult result = diagnosticsService.lookupAirport(" pvg ");

        assertEquals("PVG", result.iataCode());
        assertEquals("FOUND", result.status());
        assertNotNull(result.airport());
        assertEquals(3L, result.airport().outgoingRouteCount());
        assertEquals(4L, result.airport().incomingRouteCount());
        assertEquals(7L, result.airport().totalRouteCount());
        assertEquals("READY", result.airport().currentSnapshotStatus());
        assertTrue(result.airport().presentInCurrentSnapshot());
        assertEquals("snapshot-v1", result.airport().currentSnapshotVersion());
    }

    private static McpToolDescriptor descriptor(String toolId) {
        return new McpToolDescriptor(toolId, toolId, toolId + " description", Map.of(), Map.of());
    }

    private static GraphSnapshot snapshotWithPvgNode() {
        return new GraphSnapshot(
                "schema-1",
                "snapshot-v1",
                Instant.parse("2026-04-06T10:00:00Z"),
                "flight-main",
                "fp-1",
                List.of(new GraphSnapshotNode(
                        "N1",
                        "PVG",
                        "Pudong",
                        "Shanghai",
                        "CN",
                        31.14,
                        121.81,
                        "Asia/Shanghai",
                        false,
                        false,
                        100,
                        60,
                        Map.of())),
                List.of(new GraphSnapshotEdge(
                        "E1",
                        "N1",
                        "N2",
                        "MU",
                        "China Eastern",
                        "FULL_SERVICE",
                        1200,
                        180,
                        1800,
                        0.9,
                        true,
                        2,
                        0,
                        Map.of())),
                Map.of("source", "etl"));
    }
}




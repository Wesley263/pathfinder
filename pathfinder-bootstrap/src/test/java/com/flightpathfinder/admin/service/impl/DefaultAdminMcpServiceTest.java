package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminMcpToolDetailResult;
import com.flightpathfinder.admin.service.AdminMcpToolListResult;
import com.flightpathfinder.admin.service.AdminMcpToolSummaryItem;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotNode;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.mcp.client.McpClientProperties;
import com.flightpathfinder.rag.core.mcp.client.McpToolDiscoveryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultAdminMcpServiceTest {

    @Mock
    private LocalMcpToolRegistry localMcpToolRegistry;

    @Mock
    private McpToolDiscoveryService mcpToolDiscoveryService;

    @Mock
    private GraphSnapshotQueryService graphSnapshotQueryService;

    private DefaultAdminMcpService mcpService;

    @BeforeEach
    void setUp() {
        GraphSnapshotProperties graphSnapshotProperties = new GraphSnapshotProperties();
        graphSnapshotProperties.setDefaultGraphKey("flight-main");

        McpClientProperties mcpClientProperties = new McpClientProperties();
        mcpClientProperties.setBaseUrl("http://localhost:18081");

        mcpService = new DefaultAdminMcpService(
                localMcpToolRegistry,
                mcpToolDiscoveryService,
                graphSnapshotQueryService,
                graphSnapshotProperties,
                mcpClientProperties);
    }

    @Test
    void listTools_shouldUseLocalCatalogAndComputeAvailability() {
        when(localMcpToolRegistry.listTools()).thenReturn(List.of(
                descriptor("graph.path.search", "Graph Path Search"),
                descriptor("flight.search", "Flight Search")));
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.of(snapshot()));

        AdminMcpToolListResult result = mcpService.listTools(false);

        assertEquals("SUCCESS", result.status());
        assertEquals(6, result.count());
        assertEquals(2, result.availableCount());
        assertTrue(result.message().contains("2/6"));

        AdminMcpToolSummaryItem graphTool = findSummary(result, "graph.path.search");
        assertTrue(graphTool.available());
        assertEquals("SNAPSHOT_READY", graphTool.dependencyStatus());

        AdminMcpToolSummaryItem cityCostTool = findSummary(result, "city.cost");
        assertFalse(cityCostTool.available());
        assertEquals("REMOTE_DEPENDENCY_UNKNOWN", cityCostTool.dependencyStatus());

        verify(mcpToolDiscoveryService, never()).refreshToolCatalog();
    }

    @Test
    void listTools_shouldFallbackToLocalRegistryWhenRefreshReturnsEmpty() {
        when(mcpToolDiscoveryService.refreshToolCatalog()).thenReturn(List.of());
        when(localMcpToolRegistry.listTools()).thenReturn(List.of(descriptor("graph.path.search", "Graph Path Search")));
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.empty());

        AdminMcpToolListResult result = mcpService.listTools(true);

        assertEquals(1, result.availableCount());
        AdminMcpToolSummaryItem graphTool = findSummary(result, "graph.path.search");
        assertEquals("SNAPSHOT_MISS", graphTool.dependencyStatus());
        verify(mcpToolDiscoveryService).refreshToolCatalog();
        verify(localMcpToolRegistry).listTools();
    }

    @Test
    void findTool_shouldReturnNotFoundForUnknownTool() {
        AdminMcpToolDetailResult result = mcpService.findTool(" unknown ", false);

        assertEquals("unknown", result.toolId());
        assertEquals("NOT_FOUND", result.status());
        assertTrue(result.message().contains("toolId=unknown"));
        assertNull(result.detail());
    }

    @Test
    void findTool_shouldReturnUnavailableDetailWhenGraphSnapshotMissing() {
        when(localMcpToolRegistry.listTools()).thenReturn(List.of());
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.empty());

        AdminMcpToolDetailResult result = mcpService.findTool("graph.path.search", false);

        assertEquals("SUCCESS", result.status());
        assertNotNull(result.detail());
        assertFalse(result.detail().available());
        assertEquals("SNAPSHOT_MISS_AND_TOOL_UNAVAILABLE", result.detail().dependencyStatus());
        assertEquals("http://localhost:18081", result.detail().remoteBaseUrl());
        assertEquals(Map.of(), result.detail().inputSchema());
        assertTrue(result.detail().dependencyDetails().stream().anyMatch(item -> item.contains("snapshot is missing")));
    }

    private static McpToolDescriptor descriptor(String toolId, String displayName) {
        return new McpToolDescriptor(
                toolId,
                displayName,
                displayName + " description",
                Map.of("type", "object"),
                Map.of("type", "object"));
    }

    private static AdminMcpToolSummaryItem findSummary(AdminMcpToolListResult result, String toolId) {
        return result.tools().stream()
                .filter(item -> toolId.equals(item.toolId()))
                .findFirst()
                .orElseThrow();
    }

    private static GraphSnapshot snapshot() {
        return new GraphSnapshot(
                "schema-1",
                "snapshot-v1",
                Instant.parse("2026-04-06T10:00:00Z"),
                "flight-main",
                "fingerprint",
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




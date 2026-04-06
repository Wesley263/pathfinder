package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminCacheInvalidateResult;
import com.flightpathfinder.admin.service.AdminDataReloadResult;
import com.flightpathfinder.admin.service.AdminDataStats;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import com.flightpathfinder.admin.service.etl.AdminDatasetImporter;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotLifecycleService;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultAdminDataServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GraphSnapshotLifecycleService graphSnapshotLifecycleService;

    @Mock
    private LocalMcpToolRegistry localMcpToolRegistry;

    private GraphSnapshotProperties graphSnapshotProperties;

    @BeforeEach
    void setUp() {
        graphSnapshotProperties = new GraphSnapshotProperties();
        graphSnapshotProperties.setDefaultGraphKey("flight-main");
    }

    @Test
    void reload_shouldSortDatasetsAndInvalidateCachesWhenMaterialChangeExists() {
        AdminDatasetImporter visaImporter = mock(AdminDatasetImporter.class);
        when(visaImporter.datasetId()).thenReturn("visa");
        when(visaImporter.reload()).thenReturn(datasetResult("visa", "MISSING", 0));

        AdminDatasetImporter openflightsImporter = mock(AdminDatasetImporter.class);
        when(openflightsImporter.datasetId()).thenReturn("openflights");
        when(openflightsImporter.reload()).thenReturn(datasetResult("openflights", "SUCCESS", 2));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(localMcpToolRegistry.listTools()).thenReturn(List.of(
                new McpToolDescriptor("tool-1", "Tool 1", "d1", Map.of(), Map.of()),
                new McpToolDescriptor("tool-2", "Tool 2", "d2", Map.of(), Map.of())));

        DefaultAdminDataService dataService = new DefaultAdminDataService(
                jdbcTemplate,
                stringRedisTemplate,
                graphSnapshotLifecycleService,
                graphSnapshotProperties,
                localMcpToolRegistry,
                List.of(visaImporter, openflightsImporter));

        AdminDataReloadResult result = dataService.reload("", " ");

        assertEquals("flight-main", result.graphKey());
        assertEquals("PARTIAL_SUCCESS", result.overallStatus());
        assertEquals("manual admin data reload", result.reason());
        assertTrue(result.graphSnapshotInvalidated());
        assertEquals(List.of("graph-snapshot", "mcp-tool-registry"), result.cacheInvalidatedScopes());
        assertEquals("openflights", result.datasetResults().get(0).datasetId());
        assertEquals("visa", result.datasetResults().get(1).datasetId());
        assertEquals(5, result.stats().cityCostCount());

        verify(graphSnapshotLifecycleService).invalidate("flight-main");
        verify(localMcpToolRegistry).replaceAll(List.of());
    }

    @Test
    void reload_shouldConvertImporterExceptionToFailedDatasetWithoutInvalidation() {
        AdminDatasetImporter brokenImporter = mock(AdminDatasetImporter.class);
        when(brokenImporter.datasetId()).thenReturn("openflights");
        when(brokenImporter.reload()).thenThrow(new RuntimeException("boom"));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L, 0L, 0L, 0L, 0L);

        DefaultAdminDataService dataService = new DefaultAdminDataService(
                jdbcTemplate,
                stringRedisTemplate,
                graphSnapshotLifecycleService,
                graphSnapshotProperties,
                localMcpToolRegistry,
                List.of(brokenImporter));

        AdminDataReloadResult result = dataService.reload("flight-main", "manual");

        assertEquals("FAILED", result.overallStatus());
        assertEquals("manual", result.reason());
        assertFalse(result.graphSnapshotInvalidated());
        assertEquals(1, result.datasetResults().size());
        assertEquals("FAILED", result.datasetResults().get(0).status());
        assertTrue(result.datasetResults().get(0).reason().contains("boom"));

        verify(graphSnapshotLifecycleService, never()).invalidate("flight-main");
        verify(localMcpToolRegistry, never()).replaceAll(List.of());
    }

    @Test
    void currentStats_shouldMapNullToZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L, 20L, null, 40L, 50L);

        DefaultAdminDataService dataService = new DefaultAdminDataService(
                jdbcTemplate,
                stringRedisTemplate,
                graphSnapshotLifecycleService,
                graphSnapshotProperties,
                localMcpToolRegistry,
                List.of());

        AdminDataStats stats = dataService.currentStats();

        assertEquals(10L, stats.airportCount());
        assertEquals(20L, stats.airlineCount());
        assertEquals(0L, stats.routeCount());
        assertEquals(40L, stats.visaPolicyCount());
        assertEquals(50L, stats.cityCostCount());
    }

    @Test
    void invalidateCaches_shouldReturnClearedCounts() {
        when(stringRedisTemplate.keys(anyString())).thenReturn(Set.of("k1", "k2", "k3"));
        when(localMcpToolRegistry.listTools()).thenReturn(List.of(
                new McpToolDescriptor("tool-1", "Tool 1", "d1", Map.of(), Map.of()),
                new McpToolDescriptor("tool-2", "Tool 2", "d2", Map.of(), Map.of())));

        DefaultAdminDataService dataService = new DefaultAdminDataService(
                jdbcTemplate,
                stringRedisTemplate,
                graphSnapshotLifecycleService,
                graphSnapshotProperties,
                localMcpToolRegistry,
                List.of());

        AdminCacheInvalidateResult result = dataService.invalidateCaches(null, " ");

        assertEquals("flight-main", result.graphKey());
        assertEquals("INVALIDATED", result.status());
        assertEquals("manual admin cache invalidate", result.reason());
        assertEquals(3L, result.clearedGraphSnapshotKeyCount());
        assertEquals(2, result.clearedMcpToolCount());
        assertEquals(List.of("graph-snapshot", "mcp-tool-registry"), result.clearedScopes());

        verify(graphSnapshotLifecycleService).invalidate("flight-main");
        verify(localMcpToolRegistry).replaceAll(List.of());
    }

    private static AdminDatasetReloadResult datasetResult(String datasetId, String status, long upserted) {
        Instant now = Instant.parse("2026-04-06T10:00:00Z");
        return new AdminDatasetReloadResult(
                datasetId,
                status,
                status.toLowerCase(),
                List.of("source"),
                10,
                upserted,
                0,
                now,
                now,
                Map.of());
    }
}




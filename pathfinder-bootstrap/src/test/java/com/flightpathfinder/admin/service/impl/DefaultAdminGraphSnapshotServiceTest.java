package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminGraphSnapshotStatus;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotLifecycleService;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotNode;
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
class DefaultAdminGraphSnapshotServiceTest {

    @Mock
    private GraphSnapshotLifecycleService graphSnapshotLifecycleService;

    @Mock
    private GraphSnapshotQueryService graphSnapshotQueryService;

    private DefaultAdminGraphSnapshotService graphSnapshotService;

    @BeforeEach
    void setUp() {
        GraphSnapshotProperties properties = new GraphSnapshotProperties();
        properties.setDefaultGraphKey("flight-main");
        graphSnapshotService = new DefaultAdminGraphSnapshotService(
                graphSnapshotLifecycleService,
                graphSnapshotQueryService,
                properties);
    }

    @Test
    void currentSnapshot_shouldReturnSnapshotMissWhenNoSnapshotExists() {
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.empty());

        AdminGraphSnapshotStatus status = graphSnapshotService.currentSnapshot(" ");

        assertEquals("flight-main", status.graphKey());
        assertEquals("SNAPSHOT_MISS", status.status());
        assertEquals(0, status.nodeCount());
        assertTrue(status.reason().contains("flight-main"));
    }

    @Test
    void currentSnapshot_shouldMapReadySnapshot() {
        GraphSnapshot snapshot = snapshot("flight-main", "v1");
        when(graphSnapshotQueryService.loadCurrent("flight-main")).thenReturn(Optional.of(snapshot));

        AdminGraphSnapshotStatus status = graphSnapshotService.currentSnapshot("flight-main");

        assertEquals("READY", status.status());
        assertEquals("v1", status.snapshotVersion());
        assertEquals("schema-1", status.schemaVersion());
        assertEquals(1, status.nodeCount());
        assertEquals(1, status.edgeCount());
        assertEquals("current snapshot loaded", status.reason());
    }

    @Test
    void rebuild_shouldUseDefaultReasonWhenReasonBlank() {
        GraphSnapshot snapshot = snapshot("flight-main", "v2");
        when(graphSnapshotLifecycleService.rebuild("flight-main", "manual admin rebuild")).thenReturn(snapshot);

        AdminGraphSnapshotStatus status = graphSnapshotService.rebuild("", "  ");

        assertEquals("REBUILT", status.status());
        assertEquals("manual admin rebuild", status.reason());
        assertEquals("v2", status.snapshotVersion());
        verify(graphSnapshotLifecycleService).rebuild("flight-main", "manual admin rebuild");
    }

    @Test
    void invalidate_shouldUseResolvedGraphKeyAndNormalizedReason() {
        AdminGraphSnapshotStatus status = graphSnapshotService.invalidate(" custom-key ", " manual ");

        assertEquals("custom-key", status.graphKey());
        assertEquals("INVALIDATED", status.status());
        assertEquals("manual", status.reason());
        verify(graphSnapshotLifecycleService).invalidate("custom-key");
    }

    private static GraphSnapshot snapshot(String graphKey, String version) {
        return new GraphSnapshot(
                "schema-1",
                version,
                Instant.parse("2026-04-06T10:00:00Z"),
                graphKey,
                "fingerprint-1",
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




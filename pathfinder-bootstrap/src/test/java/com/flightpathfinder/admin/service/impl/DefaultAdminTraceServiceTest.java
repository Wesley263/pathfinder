package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminTraceDetailResult;
import com.flightpathfinder.admin.service.AdminTraceRunSummary;
import com.flightpathfinder.rag.core.trace.RagTraceDetailResult;
import com.flightpathfinder.rag.core.trace.RagTraceNodeDetail;
import com.flightpathfinder.rag.core.trace.RagTraceQueryService;
import com.flightpathfinder.rag.core.trace.RagTraceRunSummary;
import com.flightpathfinder.rag.core.trace.RagTraceToolSummary;
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
class DefaultAdminTraceServiceTest {

    @Mock
    private RagTraceQueryService ragTraceQueryService;

    private DefaultAdminTraceService adminTraceService;

    @BeforeEach
    void setUp() {
        adminTraceService = new DefaultAdminTraceService(ragTraceQueryService);
    }

    @Test
    void findDetail_shouldMapPartialSnapshotAndErrorFlags() {
        RagTraceDetailResult detail = detailResult("trace-1", "SUCCESS");
        when(ragTraceQueryService.findDetail("trace-1")).thenReturn(Optional.of(detail));

        Optional<AdminTraceDetailResult> optional = adminTraceService.findDetail("trace-1");

        assertTrue(optional.isPresent());
        AdminTraceDetailResult mapped = optional.get();
        assertEquals("trace-1", mapped.run().traceId());
        assertTrue(mapped.run().partial());
        assertTrue(mapped.run().errorOccurred());
        assertEquals(1, mapped.mcpToolSummaries().size());
        assertTrue(mapped.mcpToolSummaries().getFirst().error());
        assertTrue(mapped.stages().getFirst().partial());
        assertTrue(mapped.stages().getFirst().snapshotMiss());
        assertTrue(mapped.stages().getFirst().error().contains("阶段失败"));
    }

    @Test
    void listRuns_shouldUseDetailWhenTraceDetailExists() {
        RagTraceRunSummary run = runSummary("trace-1", "SUCCESS");
        when(ragTraceQueryService.listRuns("req-1", "conv-1", 10)).thenReturn(List.of(run));
        when(ragTraceQueryService.findDetail("trace-1")).thenReturn(Optional.of(detailResult("trace-1", "SUCCESS")));

        List<AdminTraceRunSummary> runs = adminTraceService.listRuns("req-1", "conv-1", 10);

        assertEquals(1, runs.size());
        assertEquals("trace-1", runs.getFirst().traceId());
        assertEquals(1, runs.getFirst().stages().size());
        assertEquals(1, runs.getFirst().mcpToolSummaries().size());
        assertTrue(runs.getFirst().partial());
    }

    @Test
    void listRuns_shouldFallbackToRunSummaryWhenDetailMissing() {
        RagTraceRunSummary run = runSummary("trace-2", "FAILED");
        when(ragTraceQueryService.listRuns(null, null, 5)).thenReturn(List.of(run));
        when(ragTraceQueryService.findDetail("trace-2")).thenReturn(Optional.empty());

        List<AdminTraceRunSummary> runs = adminTraceService.listRuns(null, null, 5);

        assertEquals(1, runs.size());
        assertEquals("trace-2", runs.getFirst().traceId());
        assertFalse(runs.getFirst().partial());
        assertTrue(runs.getFirst().errorOccurred());
        assertTrue(runs.getFirst().stages().isEmpty());
        assertTrue(runs.getFirst().mcpToolSummaries().isEmpty());
    }

    private static RagTraceDetailResult detailResult(String traceId, String status) {
        RagTraceRunSummary run = runSummary(traceId, status);
        RagTraceNodeDetail stage = new RagTraceNodeDetail(
                "retrieval",
                "STAGE",
                "PARTIAL_FAILURE",
                "阶段失败",
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:02Z"),
                Map.of(
                        "partial", true,
                        "snapshotMissAffected", true,
                        "error", "阶段失败"));
        RagTraceToolSummary tool = new RagTraceToolSummary("graph.path.search", "INVALID_PARAMETER", "bad args", false);
        return new RagTraceDetailResult(run, List.of(stage), List.of(stage), List.of(tool));
    }

    private static RagTraceRunSummary runSummary(String traceId, String status) {
        return new RagTraceRunSummary(
                traceId,
                "req-1",
                "conv-1",
                "rag.chat.query",
                status,
                false,
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:03Z"),
                2,
                1);
    }
}




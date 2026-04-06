package com.flightpathfinder.rag.core.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceNodeRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceRunRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceToolRecord;
import com.flightpathfinder.rag.core.trace.repository.RagTraceNodeRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceRunRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceToolRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 测试用例。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRagTraceQueryServiceTest {

    @Mock
    private RagTraceRunRepository ragTraceRunRepository;

    @Mock
    private RagTraceNodeRepository ragTraceNodeRepository;

    @Mock
    private RagTraceToolRepository ragTraceToolRepository;

    private DefaultRagTraceQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new DefaultRagTraceQueryService(
                ragTraceRunRepository,
                ragTraceNodeRepository,
                ragTraceToolRepository,
                new ObjectMapper());
    }

    @Test
    void findDetail_shouldReturnEmptyWhenTraceIdBlank() {
        Optional<RagTraceDetailResult> result = queryService.findDetail("  ");

        assertTrue(result.isEmpty());
    }

    @Test
    void findDetail_shouldMapRunNodesAndTools() {
        PersistedRagTraceRunRecord runRecord = runRecord();
        PersistedRagTraceNodeRecord stageNode = new PersistedRagTraceNodeRecord(
                "trace-1",
                1,
                "stage-one",
                "STAGE",
                "SUCCESS",
                "stage ok",
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:01Z"),
                "{\"status\":\"SUCCESS\",\"partial\":true}");
        PersistedRagTraceNodeRecord internalNode = new PersistedRagTraceNodeRecord(
                "trace-1",
                2,
                "mcp-execution",
                "INTERNAL",
                "PARTIAL_FAILURE",
                "toolCount=1",
                Instant.parse("2026-04-06T10:00:01Z"),
                Instant.parse("2026-04-06T10:00:02Z"),
                "{\"snapshotMiss\":true}");
        PersistedRagTraceToolRecord toolRecord = new PersistedRagTraceToolRecord(
                "trace-1",
                1,
                "graph.path.search",
                "SNAPSHOT_MISS",
                "not ready",
                true);

        when(ragTraceRunRepository.findByTraceId("trace-1")).thenReturn(Optional.of(runRecord));
        when(ragTraceNodeRepository.findByTraceId("trace-1")).thenReturn(List.of(stageNode, internalNode));
        when(ragTraceToolRepository.findByTraceId("trace-1")).thenReturn(List.of(toolRecord));

        Optional<RagTraceDetailResult> optional = queryService.findDetail("trace-1");

        assertTrue(optional.isPresent());
        RagTraceDetailResult detail = optional.get();
        assertEquals("trace-1", detail.run().traceId());
        assertEquals(1, detail.stages().size());
        assertEquals(2, detail.nodes().size());
        assertEquals(1, detail.mcpToolSummaries().size());
        assertEquals(true, detail.stages().getFirst().attributes().get("partial"));
        assertEquals(true, detail.nodes().get(1).attributes().get("snapshotMiss"));
    }

    @Test
    void findDetail_shouldFallbackRawAttributesWhenJsonInvalid() {
        PersistedRagTraceRunRecord runRecord = runRecord();
        PersistedRagTraceNodeRecord brokenNode = new PersistedRagTraceNodeRecord(
                "trace-1",
                1,
                "stage-one",
                "STAGE",
                "SUCCESS",
                "stage ok",
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:01Z"),
                "{not-json}");

        when(ragTraceRunRepository.findByTraceId("trace-1")).thenReturn(Optional.of(runRecord));
        when(ragTraceNodeRepository.findByTraceId("trace-1")).thenReturn(List.of(brokenNode));
        when(ragTraceToolRepository.findByTraceId("trace-1")).thenReturn(List.of());

        RagTraceDetailResult detail = queryService.findDetail("trace-1").orElseThrow();

        assertEquals("{not-json}", detail.nodes().getFirst().attributes().get("rawAttributes"));
    }

    @Test
    void listRuns_shouldDelegateAndMapSummary() {
        when(ragTraceRunRepository.findRecent("req-1", "conv-1", 20)).thenReturn(List.of(runRecord()));

        List<RagTraceRunSummary> summaries = queryService.listRuns("req-1", "conv-1", 20);

        assertEquals(1, summaries.size());
        assertEquals("trace-1", summaries.getFirst().traceId());
        verify(ragTraceRunRepository).findRecent("req-1", "conv-1", 20);
    }

    private static PersistedRagTraceRunRecord runRecord() {
        return new PersistedRagTraceRunRecord(
                "trace-1",
                "req-1",
                "conv-1",
                "rag.chat.query",
                "SUCCESS",
                false,
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:02Z"),
                2,
                1);
    }
}


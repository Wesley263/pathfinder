package com.flightpathfinder.rag.core.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceNodeRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceRunRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceToolRecord;
import com.flightpathfinder.rag.core.trace.repository.RagTraceNodeRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceRunRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceToolRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 测试用例。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRagTraceRecordServiceTest {

    @Mock
    private RagTraceRunRepository ragTraceRunRepository;

    @Mock
    private RagTraceNodeRepository ragTraceNodeRepository;

    @Mock
    private RagTraceToolRepository ragTraceToolRepository;

    private DefaultRagTraceRecordService recordService;

    @BeforeEach
    void setUp() {
        recordService = new DefaultRagTraceRecordService(
                ragTraceRunRepository,
                ragTraceNodeRepository,
                ragTraceToolRepository,
                new ObjectMapper());
    }

    @Test
    void persist_shouldNoOpWhenTraceIdIsBlank() {
        RagTraceResult traceResult = new RagTraceResult(
                "",
                "req-1",
                "conv-1",
                "scene",
                "SUCCESS",
                false,
                null,
                List.of(),
                List.of());

        recordService.persist(traceResult);

        verifyNoInteractions(ragTraceRunRepository, ragTraceNodeRepository, ragTraceToolRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void persist_shouldMapRunNodesAndToolsToRepositories() {
        Instant startedAt = Instant.parse("2026-04-06T10:00:00Z");
        Instant stageFinishedAt = Instant.parse("2026-04-06T10:00:01Z");
        Instant internalFinishedAt = Instant.parse("2026-04-06T10:00:03Z");
        TraceRoot root = new TraceRoot(
                "trace-1",
                "rag.chat.query",
                startedAt,
                List.of(
                        new TraceNode("stage-one", startedAt, stageFinishedAt, Map.of("status", "SUCCESS", "summary", "stage ok")),
                        new TraceNode("mcp-execution", stageFinishedAt, internalFinishedAt, Map.of("status", "PARTIAL_FAILURE", "toolCount", 1))));
        RagTraceResult traceResult = new RagTraceResult(
                "trace-1",
                "req-1",
                "conv-1",
                "rag.chat.query",
                "PARTIAL_SUCCESS",
                true,
                root,
                List.of(),
                List.of(new RagTraceToolSummary("flight.search", "NO_FLIGHTS_FOUND", "none", false)));

        recordService.persist(traceResult);

        ArgumentCaptor<PersistedRagTraceRunRecord> runCaptor = ArgumentCaptor.forClass(PersistedRagTraceRunRecord.class);
        ArgumentCaptor<List<PersistedRagTraceNodeRecord>> nodeCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PersistedRagTraceToolRecord>> toolCaptor = ArgumentCaptor.forClass(List.class);

        verify(ragTraceRunRepository).upsert(runCaptor.capture());
        verify(ragTraceNodeRepository).replaceForTrace(eq("trace-1"), nodeCaptor.capture());
        verify(ragTraceToolRepository).replaceForTrace(eq("trace-1"), toolCaptor.capture());

        PersistedRagTraceRunRecord runRecord = runCaptor.getValue();
        assertEquals("trace-1", runRecord.traceId());
        assertEquals("PARTIAL_SUCCESS", runRecord.overallStatus());
        assertTrue(runRecord.snapshotMissOccurred());
        assertEquals(startedAt, runRecord.startedAt());
        assertEquals(internalFinishedAt, runRecord.finishedAt());
        assertEquals(2, runRecord.nodeCount());
        assertEquals(1, runRecord.toolCount());

        List<PersistedRagTraceNodeRecord> nodeRecords = nodeCaptor.getValue();
        assertEquals(2, nodeRecords.size());
        assertEquals("STAGE", nodeRecords.get(0).nodeType());
        assertEquals("stage ok", nodeRecords.get(0).summary());
        assertEquals("INTERNAL", nodeRecords.get(1).nodeType());
        assertEquals("toolCount=1", nodeRecords.get(1).summary());
        assertTrue(nodeRecords.get(0).attributesJson().contains("\"status\":\"SUCCESS\""));

        List<PersistedRagTraceToolRecord> toolRecords = toolCaptor.getValue();
        assertEquals(1, toolRecords.size());
        assertEquals("flight.search", toolRecords.get(0).toolId());
        assertEquals("NO_FLIGHTS_FOUND", toolRecords.get(0).status());
        assertEquals("none", toolRecords.get(0).message());
    }
}


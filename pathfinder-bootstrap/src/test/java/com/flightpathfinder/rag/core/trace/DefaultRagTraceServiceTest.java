package com.flightpathfinder.rag.core.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.framework.trace.TraceRoot;
import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRagTraceServiceTest {

    @Mock
    private RagTraceRecorder ragTraceRecorder;

    @Mock
    private RagTraceRecordService ragTraceRecordService;

    private DefaultRagTraceService traceService;

    @BeforeEach
    void setUp() {
        traceService = new DefaultRagTraceService(ragTraceRecorder, ragTraceRecordService);
    }

    @Test
    void startQueryTrace_shouldCreateSessionFromRecorderRoot() {
        TraceRoot root = new TraceRoot("trace-1", "rag.chat.query", Instant.now(), new ArrayList<>());
        when(ragTraceRecorder.startRoot("rag.chat.query")).thenReturn(root);

        RagTraceSession session = traceService.startQueryTrace("req-1", "conv-1");

        assertEquals("req-1", session.requestId());
        assertEquals("conv-1", session.conversationId());
        assertEquals(root, session.root());
    }

    @Test
    void recordRetrieval_shouldAppendRetrievalAndMcpExecutionNodesAndCollectToolSummary() {
        RagTraceSession session = session("trace-1");
        Instant startedAt = Instant.parse("2026-04-06T10:00:00Z");
        RetrievalResult retrievalResult = new RetrievalResult(
                "SNAPSHOT_MISS",
                "kb=EMPTY(0) | mcp=PARTIAL_FAILURE(1)",
                stageOne("改写问题"),
                KbContext.empty("no kb", List.of()),
                new McpContext("PARTIAL_FAILURE", "snapshot miss", List.of(), List.of(new McpExecutionRecord(
                        "mcp-1",
                        "路径规划",
                        "子问题",
                        "graph.path.search",
                        "LOCAL",
                        Map.of("origin", "PEK", "destination", "NRT"),
                        false,
                        "SNAPSHOT_MISS",
                        "snapshot not ready",
                        "",
                        true,
                        Boolean.TRUE,
                        "RETRY_LATER",
                        null))));

        traceService.recordRetrieval(session, startedAt, retrievalResult);

        ArgumentCaptor<String> nodeNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(ragTraceRecorder, times(2))
                .appendNode(eq(session.root()), nodeNameCaptor.capture(), eq(startedAt), any(Instant.class), any(Map.class));
        assertEquals(List.of("retrieval", "mcp-execution"), nodeNameCaptor.getAllValues());
        assertEquals(1, session.stageResults().size());
        assertEquals("retrieval", session.stageResults().getFirst().stageName());
        assertEquals(1, session.mcpToolSummaries().size());
        assertEquals("graph.path.search", session.mcpToolSummaries().getFirst().toolId());
        assertTrue(session.mcpToolSummaries().getFirst().snapshotMiss());
    }

    @Test
    void finish_shouldReturnResultEvenWhenPersistThrows() {
        RagTraceSession session = session("trace-1");
        session.stageResults().add(new RagTraceStageResult(
                "stage-one",
                "SUCCESS",
                "ok",
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:00:01Z"),
                Map.of("status", "SUCCESS")));
        doThrow(new IllegalStateException("db unavailable"))
                .when(ragTraceRecordService)
                .persist(any(RagTraceResult.class));

        RagTraceResult result = traceService.finish(session, "SUCCESS", false);

        assertEquals("trace-1", result.traceId());
        assertEquals("SUCCESS", result.overallStatus());
        assertFalse(result.snapshotMissOccurred());
        verify(ragTraceRecordService).persist(any(RagTraceResult.class));
    }

    @Test
    void clear_shouldDelegateToRecorder() {
        traceService.clear();

        verify(ragTraceRecorder).clear();
    }

    @Test
    void finish_shouldPropagateWhenRootIsMissing() {
        RagTraceSession session = new RagTraceSession("req-1", "conv-1", null);

        assertThrows(NullPointerException.class, () -> traceService.finish(session, "FAILED", false));
    }

    private static RagTraceSession session(String traceId) {
        return new RagTraceSession("req-1", "conv-1", new TraceRoot(traceId, "rag.chat.query", Instant.now(), new ArrayList<>()));
    }

    private static StageOneRagResult stageOne(String rewrittenQuestion) {
        RewriteResult rewriteResult = new RewriteResult(
                rewrittenQuestion,
                List.of(rewrittenQuestion),
                rewrittenQuestion,
                List.of(rewrittenQuestion));
        IntentSplitResult splitResult = IntentSplitResult.empty();
        return new StageOneRagResult(
                rewriteResult,
                new IntentResolution(List.of(), splitResult),
                splitResult,
                ConversationMemoryContext.empty("conv-1"));
    }
}




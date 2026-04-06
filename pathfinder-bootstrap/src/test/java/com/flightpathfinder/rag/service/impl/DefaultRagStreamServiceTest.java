package com.flightpathfinder.rag.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.framework.trace.TraceRoot;
import com.flightpathfinder.rag.core.answer.AnswerEvidenceSummary;
import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.answer.FinalAnswerService;
import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.memory.ConversationMemoryService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryWriteRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagPipeline;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import com.flightpathfinder.rag.core.trace.RagTraceService;
import com.flightpathfinder.rag.core.trace.RagTraceSession;
import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEvent;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
/**
 * 测试用例。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRagStreamServiceTest {

    @Mock
    private StageOneRagPipeline stageOneRagPipeline;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private FinalAnswerService finalAnswerService;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private RagTraceService ragTraceService;

    @Mock
    private ObjectProvider<TaskExecutor> taskExecutorProvider;

    private DefaultRagStreamService streamService;

    @BeforeEach
    void setUp() {
        when(taskExecutorProvider.getIfAvailable(org.mockito.ArgumentMatchers.<java.util.function.Supplier<TaskExecutor>>any()))
                .thenReturn(Runnable::run);
        streamService = new DefaultRagStreamService(
                stageOneRagPipeline,
                retrievalService,
                finalAnswerService,
                conversationMemoryService,
                ragTraceService,
                taskExecutorProvider);
    }

    @Test
    void stream_shouldEmitStageEventsAndCompleteOnSuccess() {
        RagStreamCommand command = new RagStreamCommand("问题", "conv-1", "req-1");
        CapturingWriter writer = new CapturingWriter();

        ConversationMemoryContext memoryContext = ConversationMemoryContext.empty("conv-1");
        StageOneRagResult stageOneResult = stageOneResult("改写问题", "conv-1");
        RetrievalResult retrievalResult = retrievalResult(stageOneResult, "SUCCESS", false, List.of());
        AnswerResult answerResult = new AnswerResult(
                "SUCCESS",
                "这是最终回答",
                false,
                false,
                false,
                List.of(new AnswerEvidenceSummary("KB", "kb-doc", "label", "SUCCESS", "snippet")));
        RagTraceSession traceSession = traceSession("trace-1", "req-1", "conv-1");
        RagTraceResult traceResult = new RagTraceResult(
                "trace-1",
                "req-1",
                "conv-1",
                "RAG_STREAM",
                "SUCCESS",
                false,
                traceSession.root(),
                List.of(),
                List.of());

        when(ragTraceService.startQueryTrace("req-1", "conv-1")).thenReturn(traceSession);
        when(conversationMemoryService.loadContext("conv-1")).thenReturn(memoryContext);
        when(stageOneRagPipeline.run(any(StageOneRagRequest.class))).thenReturn(stageOneResult);
        when(retrievalService.retrieve(stageOneResult)).thenReturn(retrievalResult);
        when(finalAnswerService.answer(retrievalResult)).thenReturn(answerResult);
        when(ragTraceService.finish(traceSession, "SUCCESS", false)).thenReturn(traceResult);

        streamService.stream(command, writer);

        assertTrue(writer.completed);
        List<String> eventNames = writer.events.stream().map(RagStreamEvent::event).toList();
        assertEquals(List.of(
                "stage_one_started",
                "stage_one_completed",
                "retrieval_started",
                "retrieval_completed",
                "final_answer_started",
                "final_answer_chunk",
                "final_answer_completed"), eventNames);

        RagStreamEvent finalEvent = writer.events.get(writer.events.size() - 1);
        assertEquals("SUCCESS", finalEvent.status());
        assertEquals("这是最终回答", finalEvent.data().get("answerText"));

        verify(conversationMemoryService).appendTurn(any(ConversationMemoryWriteRequest.class));
        verify(ragTraceService).recordStageOne(eq(traceSession), any(Instant.class), same(stageOneResult));
        verify(ragTraceService).recordRetrieval(eq(traceSession), any(Instant.class), same(retrievalResult));
        verify(ragTraceService).recordFinalAnswer(eq(traceSession), any(Instant.class), same(answerResult));
        verify(ragTraceService).finish(traceSession, "SUCCESS", false);
        verify(ragTraceService).clear();
    }

    @Test
    void stream_shouldEmitErrorEventAndCompleteWhenRetrievalFails() {
        RagStreamCommand command = new RagStreamCommand("问题", "conv-1", "req-1");
        CapturingWriter writer = new CapturingWriter();
        ConversationMemoryContext memoryContext = ConversationMemoryContext.empty("conv-1");
        StageOneRagResult stageOneResult = stageOneResult("改写问题", "conv-1");
        RagTraceSession traceSession = traceSession("trace-1", "req-1", "conv-1");
        RuntimeException failure = new IllegalStateException("retrieval boom");

        when(ragTraceService.startQueryTrace("req-1", "conv-1")).thenReturn(traceSession);
        when(conversationMemoryService.loadContext("conv-1")).thenReturn(memoryContext);
        when(stageOneRagPipeline.run(any(StageOneRagRequest.class))).thenReturn(stageOneResult);
        when(retrievalService.retrieve(stageOneResult)).thenThrow(failure);

        streamService.stream(command, writer);

        assertTrue(writer.completed);
        assertFalse(writer.events.isEmpty());
        RagStreamEvent errorEvent = writer.events.get(writer.events.size() - 1);
        assertEquals("error", errorEvent.event());
        assertEquals("FAILED", errorEvent.status());
        assertTrue(String.valueOf(errorEvent.data().get("message")).contains("retrieval boom"));

        verify(ragTraceService).recordFailure(eq(traceSession), eq("retrieval"), any(Instant.class), same(failure));
        verify(ragTraceService).finish(traceSession, "FAILED", false);
        verify(ragTraceService).clear();
        verify(finalAnswerService, never()).answer(any(RetrievalResult.class));
        verify(conversationMemoryService, never()).appendTurn(any(ConversationMemoryWriteRequest.class));
    }

    private static StageOneRagResult stageOneResult(String rewrittenQuestion, String conversationId) {
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
                ConversationMemoryContext.empty(conversationId));
    }

    private static RetrievalResult retrievalResult(
            StageOneRagResult stageOneResult,
            String status,
            boolean snapshotMiss,
            List<McpExecutionRecord> executions) {
        KbContext kbContext = KbContext.empty("no kb", List.of());
        McpContext mcpContext = new McpContext(status, "summary", List.of(), executions);
        if (snapshotMiss) {
            mcpContext = new McpContext(status, "summary", List.of(), List.of(new McpExecutionRecord(
                    "mcp-1",
                    "工具意图",
                    "子问题",
                    "graph.path.search",
                    "LOCAL",
                    Map.of(),
                    false,
                    "SNAPSHOT_MISS",
                    "snapshot miss",
                    "",
                    true,
                    Boolean.TRUE,
                    "RETRY_LATER",
                    null)));
        }
        return new RetrievalResult(status, "summary", stageOneResult, kbContext, mcpContext);
    }

    private static RagTraceSession traceSession(String traceId, String requestId, String conversationId) {
        TraceRoot root = new TraceRoot(traceId, "RAG_STREAM", Instant.now(), List.of());
        return new RagTraceSession(requestId, conversationId, root);
    }

    private static final class CapturingWriter implements RagStreamEventWriter {

        private final List<RagStreamEvent> events = new ArrayList<>();
        private boolean completed;

        @Override
        public void emit(RagStreamEvent event) {
            events.add(event);
        }

        @Override
        public void complete() {
            completed = true;
        }
    }
}


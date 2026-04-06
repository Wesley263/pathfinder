package com.flightpathfinder.rag.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import com.flightpathfinder.rag.core.trace.RagTraceService;
import com.flightpathfinder.rag.core.trace.RagTraceSession;
import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 测试用例。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRagQueryServiceTest {

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

    private DefaultRagQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new DefaultRagQueryService(
                stageOneRagPipeline,
                retrievalService,
                finalAnswerService,
                conversationMemoryService,
                ragTraceService);
    }

    @Test
    void query_shouldOrchestrateAllStagesAndWriteMemory() {
        RagQueryCommand command = new RagQueryCommand("问题", "conv-1", "req-1");
        ConversationMemoryContext memoryContext = ConversationMemoryContext.empty("conv-1");
        StageOneRagResult stageOneResult = stageOneResult("改写问题", "conv-1");
        RetrievalResult retrievalResult = retrievalResult(stageOneResult);
        AnswerResult answerResult = new AnswerResult("SUCCESS", "最终回答", false, false, false, List.of());
        RagTraceSession traceSession = new RagTraceSession("req-1", "conv-1", null);
        RagTraceResult traceResult = new RagTraceResult(
                "trace-1",
                "req-1",
                "conv-1",
                "RAG_QUERY",
                "SUCCESS",
                false,
                null,
                List.of(),
                List.of());

        when(ragTraceService.startQueryTrace("req-1", "conv-1")).thenReturn(traceSession);
        when(conversationMemoryService.loadContext("conv-1")).thenReturn(memoryContext);
        when(stageOneRagPipeline.run(any(StageOneRagRequest.class))).thenReturn(stageOneResult);
        when(retrievalService.retrieve(stageOneResult)).thenReturn(retrievalResult);
        when(finalAnswerService.answer(retrievalResult)).thenReturn(answerResult);
        when(ragTraceService.finish(traceSession, "SUCCESS", false)).thenReturn(traceResult);

        RagQueryResult result = queryService.query(command);

        assertSame(stageOneResult, result.stageOneResult());
        assertSame(retrievalResult, result.retrievalResult());
        assertSame(answerResult, result.answerResult());
        assertSame(traceResult, result.traceResult());

        ArgumentCaptor<StageOneRagRequest> stageOneRequestCaptor = ArgumentCaptor.forClass(StageOneRagRequest.class);
        verify(stageOneRagPipeline).run(stageOneRequestCaptor.capture());
        StageOneRagRequest stageOneRequest = stageOneRequestCaptor.getValue();
        assertEquals("问题", stageOneRequest.originalQuestion());
        assertEquals("conv-1", stageOneRequest.conversationId());
        assertEquals("req-1", stageOneRequest.requestId());
        assertSame(memoryContext, stageOneRequest.memoryContext());

        ArgumentCaptor<ConversationMemoryWriteRequest> writeRequestCaptor =
                ArgumentCaptor.forClass(ConversationMemoryWriteRequest.class);
        verify(conversationMemoryService).appendTurn(writeRequestCaptor.capture());
        ConversationMemoryWriteRequest writeRequest = writeRequestCaptor.getValue();
        assertEquals("conv-1", writeRequest.conversationId());
        assertEquals("req-1", writeRequest.requestId());
        assertEquals("问题", writeRequest.userQuestion());
        assertEquals("改写问题", writeRequest.rewrittenQuestion());
        assertEquals("最终回答", writeRequest.answerText());
        assertEquals("SUCCESS", writeRequest.answerStatus());

        InOrder inOrder = inOrder(
                ragTraceService,
                conversationMemoryService,
                stageOneRagPipeline,
                retrievalService,
                finalAnswerService);
        inOrder.verify(ragTraceService).startQueryTrace("req-1", "conv-1");
        inOrder.verify(conversationMemoryService).loadContext("conv-1");
        inOrder.verify(stageOneRagPipeline).run(any(StageOneRagRequest.class));
        inOrder.verify(ragTraceService).recordStageOne(eq(traceSession), any(Instant.class), same(stageOneResult));
        inOrder.verify(retrievalService).retrieve(stageOneResult);
        inOrder.verify(ragTraceService).recordRetrieval(eq(traceSession), any(Instant.class), same(retrievalResult));
        inOrder.verify(finalAnswerService).answer(retrievalResult);
        inOrder.verify(ragTraceService).recordFinalAnswer(eq(traceSession), any(Instant.class), same(answerResult));
        inOrder.verify(conversationMemoryService).appendTurn(any(ConversationMemoryWriteRequest.class));
        inOrder.verify(ragTraceService).finish(traceSession, "SUCCESS", false);
        inOrder.verify(ragTraceService).clear();
    }

    @Test
    void query_shouldRecordFailureAndStillClearWhenRetrievalThrows() {
        RagQueryCommand command = new RagQueryCommand("问题", "conv-1", "req-1");
        ConversationMemoryContext memoryContext = ConversationMemoryContext.empty("conv-1");
        StageOneRagResult stageOneResult = stageOneResult("改写问题", "conv-1");
        RagTraceSession traceSession = new RagTraceSession("req-1", "conv-1", null);
        RuntimeException failure = new IllegalStateException("boom");

        when(ragTraceService.startQueryTrace("req-1", "conv-1")).thenReturn(traceSession);
        when(conversationMemoryService.loadContext("conv-1")).thenReturn(memoryContext);
        when(stageOneRagPipeline.run(any(StageOneRagRequest.class))).thenReturn(stageOneResult);
        when(retrievalService.retrieve(stageOneResult)).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> queryService.query(command));

        assertSame(failure, thrown);
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

    private static RetrievalResult retrievalResult(StageOneRagResult stageOneResult) {
        return new RetrievalResult(
                "SUCCESS",
                "summary",
                stageOneResult,
                KbContext.empty("no kb", List.of()),
                McpContext.skipped("no mcp"));
    }
}


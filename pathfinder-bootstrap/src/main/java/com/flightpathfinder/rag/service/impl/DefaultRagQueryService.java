package com.flightpathfinder.rag.service.impl;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.answer.FinalAnswerService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.memory.ConversationMemoryService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryWriteRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagPipeline;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalService;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import com.flightpathfinder.rag.core.trace.RagTraceService;
import com.flightpathfinder.rag.core.trace.RagTraceSession;
import com.flightpathfinder.rag.service.RagQueryService;
import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Default synchronous RAG application orchestrator.
 *
 * <p>This class owns the end-to-end request lifecycle: memory load, stage one, retrieval,
 * final answer, memory write, and trace completion. That boundary keeps controllers thin and
 * prevents stage-specific services from accumulating cross-cutting orchestration concerns.</p>
 */
@Service
public class DefaultRagQueryService implements RagQueryService {

    private final StageOneRagPipeline stageOneRagPipeline;
    private final RetrievalService retrievalService;
    private final FinalAnswerService finalAnswerService;
    private final ConversationMemoryService conversationMemoryService;
    private final RagTraceService ragTraceService;

    public DefaultRagQueryService(StageOneRagPipeline stageOneRagPipeline,
                                  RetrievalService retrievalService,
                                  FinalAnswerService finalAnswerService,
                                  ConversationMemoryService conversationMemoryService,
                                  RagTraceService ragTraceService) {
        this.stageOneRagPipeline = stageOneRagPipeline;
        this.retrievalService = retrievalService;
        this.finalAnswerService = finalAnswerService;
        this.conversationMemoryService = conversationMemoryService;
        this.ragTraceService = ragTraceService;
    }

    /**
     * Runs the synchronous RAG mainline and returns the full query result.
     *
     * @param command request command derived from the user-facing API
     * @return stage-one, retrieval, answer, and trace outputs for the current request
     */
    @Override
    public RagQueryResult query(RagQueryCommand command) {
        RagQueryCommand safeCommand = command == null
                ? new RagQueryCommand("", "", "")
                : command;
        // Trace starts before any stage executes so both successful stages and early failures can share the
        // same request-level trace id.
        RagTraceSession traceSession = ragTraceService.startQueryTrace(
                safeCommand.requestId(),
                safeCommand.conversationId());
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // Memory is loaded before stage one so follow-up rewrite and routing can use the
            // latest conversation context without pushing memory logic into stage services.
            ConversationMemoryContext memoryContext = conversationMemoryService.loadContext(safeCommand.conversationId());

            currentStage = "stage-one";
            currentStageStartedAt = Instant.now();
            StageOneRagResult stageOneResult = stageOneRagPipeline.run(new StageOneRagRequest(
                    safeCommand.question(),
                    safeCommand.conversationId(),
                    safeCommand.requestId(),
                    memoryContext));
            ragTraceService.recordStageOne(traceSession, currentStageStartedAt, stageOneResult);

            currentStage = "retrieval";
            currentStageStartedAt = Instant.now();
            RetrievalResult retrievalResult = retrievalService.retrieve(stageOneResult);
            ragTraceService.recordRetrieval(traceSession, currentStageStartedAt, retrievalResult);

            currentStage = "final-answer";
            currentStageStartedAt = Instant.now();
            AnswerResult answerResult = finalAnswerService.answer(retrievalResult);
            ragTraceService.recordFinalAnswer(traceSession, currentStageStartedAt, answerResult);

            // Memory write happens after the final answer is known so later turns can see the
            // exact rewritten question and answer status that this request produced.
            conversationMemoryService.appendTurn(new ConversationMemoryWriteRequest(
                    safeCommand.conversationId(),
                    safeCommand.requestId(),
                    safeCommand.question(),
                    stageOneResult.rewriteResult().rewrittenQuestion(),
                    answerResult.answerText(),
                    answerResult.status(),
                    Instant.now()));

            // Trace is finished after memory write so the persisted trace reflects the full synchronous request
            // lifecycle rather than only the model-facing stages.
            RagTraceResult traceResult = ragTraceService.finish(
                    traceSession,
                    answerResult.status(),
                    answerResult.snapshotMissAffected());
            return new RagQueryResult(stageOneResult, retrievalResult, answerResult, traceResult);
        } catch (RuntimeException exception) {
            ragTraceService.recordFailure(traceSession, currentStage, currentStageStartedAt, exception);
            ragTraceService.finish(traceSession, "FAILED", false);
            throw exception;
        } finally {
            // Clearing in finally prevents trace context leakage across requests even when persistence or
            // business logic throws.
            ragTraceService.clear();
        }
    }
}

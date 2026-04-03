package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.time.Instant;

/**
 * Lifecycle service for request-scoped RAG traces.
 *
 * <p>This abstraction keeps trace orchestration outside the business services for stage one, retrieval and
 * final answer. The application layer can therefore record stage facts without forcing each stage to manage
 * its own persistence and context lifecycle.
 */
public interface RagTraceService {

    /**
     * Starts a new request-level trace session.
     *
     * @param requestId request identifier of the current query
     * @param conversationId conversation identifier associated with the request, if any
     * @return mutable trace session used throughout the current query lifecycle
     */
    RagTraceSession startQueryTrace(String requestId, String conversationId);

    /**
     * Records the stage-one result into the active trace session.
     *
     * @param session current trace session
     * @param startedAt stage start time captured by the orchestrator
     * @param stageOneResult stage-one output to summarize into trace
     */
    void recordStageOne(RagTraceSession session, Instant startedAt, StageOneRagResult stageOneResult);

    /**
     * Records retrieval facts into the active trace session.
     *
     * @param session current trace session
     * @param startedAt stage start time captured by the orchestrator
     * @param retrievalResult retrieval output including KB and MCP summaries
     */
    void recordRetrieval(RagTraceSession session, Instant startedAt, RetrievalResult retrievalResult);

    /**
     * Records the final-answer stage into the active trace session.
     *
     * @param session current trace session
     * @param startedAt stage start time captured by the orchestrator
     * @param answerResult final answer result to summarize into trace
     */
    void recordFinalAnswer(RagTraceSession session, Instant startedAt, AnswerResult answerResult);

    /**
     * Records an abnormal stage failure before the trace is finished.
     *
     * @param session current trace session
     * @param stageName stage name that failed
     * @param startedAt stage start time captured by the orchestrator
     * @param throwable failure that terminated the stage
     */
    void recordFailure(RagTraceSession session, String stageName, Instant startedAt, Throwable throwable);

    /**
     * Finalizes the trace and attempts persistence.
     *
     * @param session current trace session
     * @param overallStatus overall request status after orchestration finishes
     * @param snapshotMissOccurred whether a snapshot miss affected the request outcome
     * @return finalized trace result for response-side audit and optional persistence
     */
    RagTraceResult finish(RagTraceSession session, String overallStatus, boolean snapshotMissOccurred);

    /**
     * Clears request-scoped trace context after orchestration completes.
     */
    void clear();
}

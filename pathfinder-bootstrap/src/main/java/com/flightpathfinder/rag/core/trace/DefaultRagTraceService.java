package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the request-scoped trace lifecycle.
 *
 * <p>This service translates stage outputs into trace nodes and tool summaries, then hands the finished trace
 * to the persistence layer. It keeps trace facts separate from business execution so empty results, snapshot
 * misses and structured business failures are still preserved as audit data.
 */
@Service
public class DefaultRagTraceService implements RagTraceService {

    private static final String SCENE = "rag.chat.query";
    private static final Logger log = LoggerFactory.getLogger(DefaultRagTraceService.class);

    private final RagTraceRecorder ragTraceRecorder;
    private final RagTraceRecordService ragTraceRecordService;

    public DefaultRagTraceService(RagTraceRecorder ragTraceRecorder,
                                  RagTraceRecordService ragTraceRecordService) {
        this.ragTraceRecorder = ragTraceRecorder;
        this.ragTraceRecordService = ragTraceRecordService;
    }

    /**
     * Starts a fresh trace root for one RAG query.
     *
     * @param requestId request identifier for the current query
     * @param conversationId conversation identifier associated with the query
     * @return mutable trace session carrying request ids and the active trace root
     */
    @Override
    public RagTraceSession startQueryTrace(String requestId, String conversationId) {
        TraceRoot root = ragTraceRecorder.startRoot(SCENE);
        return new RagTraceSession(requestId, conversationId, root);
    }

    /**
     * Records the stage-one summary into the trace session.
     *
     * @param session current trace session
     * @param startedAt stage start time
     * @param stageOneResult stage-one result to summarize
     */
    @Override
    public void recordStageOne(RagTraceSession session, Instant startedAt, StageOneRagResult stageOneResult) {
        StageOneRagResult safeStageOneResult = stageOneResult == null
                ? new StageOneRagResult(null, null, null, null)
                : stageOneResult;
        Instant finishedAt = Instant.now();
        String status = safeStageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                && safeStageOneResult.intentSplitResult().kbIntents().isEmpty()
                && safeStageOneResult.intentSplitResult().mcpIntents().isEmpty()
                && safeStageOneResult.intentSplitResult().systemIntents().isEmpty()
                ? "EMPTY"
                : "SUCCESS";
        Map<String, Object> attributes = orderedMap(
                "requestId", session.requestId(),
                "conversationId", session.conversationId(),
                "rewrittenQuestion", abbreviate(safeStageOneResult.rewriteResult().rewrittenQuestion()),
                "subQuestionCount", safeStageOneResult.rewriteResult().subQuestions().size(),
                "kbIntentCount", safeStageOneResult.intentSplitResult().kbIntents().size(),
                "mcpIntentCount", safeStageOneResult.intentSplitResult().mcpIntents().size(),
                "systemIntentCount", safeStageOneResult.intentSplitResult().systemIntents().size(),
                "memoryTurnCount", safeStageOneResult.memoryContext().recentTurns().size(),
                "status", status);
        String summary = "rewrite="
                + safeStageOneResult.rewriteResult().subQuestions().size()
                + " | kb=" + safeStageOneResult.intentSplitResult().kbIntents().size()
                + " | mcp=" + safeStageOneResult.intentSplitResult().mcpIntents().size()
                + " | system=" + safeStageOneResult.intentSplitResult().systemIntents().size();
        appendStage(session, "stage-one", status, summary, startedAt, finishedAt, attributes);
    }

    /**
     * Records KB/MCP retrieval outcomes and MCP tool summaries.
     *
     * @param session current trace session
     * @param startedAt stage start time
     * @param retrievalResult retrieval result to summarize
     */
    @Override
    public void recordRetrieval(RagTraceSession session, Instant startedAt, RetrievalResult retrievalResult) {
        RetrievalResult safeRetrievalResult = retrievalResult == null
                ? new RetrievalResult(null, null, null, null, null)
                : retrievalResult;
        Instant finishedAt = Instant.now();
        Map<String, Object> attributes = orderedMap(
                "requestId", session.requestId(),
                "conversationId", session.conversationId(),
                "status", safeRetrievalResult.status(),
                "summary", safeRetrievalResult.summary(),
                "kbStatus", safeRetrievalResult.kbContext().status(),
                "kbItemCount", safeRetrievalResult.kbContext().items().size(),
                "mcpStatus", safeRetrievalResult.mcpContext().status(),
                "mcpExecutionCount", safeRetrievalResult.mcpContext().executions().size(),
                "snapshotMiss", safeRetrievalResult.hasSnapshotMiss());
        appendStage(session, "retrieval", safeRetrievalResult.status(), safeRetrievalResult.summary(), startedAt, finishedAt, attributes);

        if (!safeRetrievalResult.mcpContext().executions().isEmpty()) {
            List<Map<String, Object>> toolSummaries = safeRetrievalResult.mcpContext().executions().stream()
                    .map(this::toToolSummaryMap)
                    .toList();
            safeRetrievalResult.mcpContext().executions().stream()
                    .map(this::toToolSummary)
                    .forEach(session.mcpToolSummaries()::add);
            // Tool execution is recorded as a separate internal node so operators can see retrieval as a stage
            // while still drilling down into MCP-specific outcomes such as snapshot miss or partial results.
            ragTraceRecorder.appendNode(
                    session.root(),
                    "mcp-execution",
                    startedAt,
                    finishedAt,
                    orderedMap(
                            "requestId", session.requestId(),
                            "conversationId", session.conversationId(),
                            "status", safeRetrievalResult.mcpContext().status(),
                            "summary", safeRetrievalResult.mcpContext().summary(),
                            "toolCount", safeRetrievalResult.mcpContext().executions().size(),
                            "tools", toolSummaries,
                            "snapshotMiss", safeRetrievalResult.hasSnapshotMiss()));
        }
    }

    /**
     * Records the final-answer stage into the trace.
     *
     * @param session current trace session
     * @param startedAt stage start time
     * @param answerResult final answer output to summarize
     */
    @Override
    public void recordFinalAnswer(RagTraceSession session, Instant startedAt, AnswerResult answerResult) {
        AnswerResult safeAnswerResult = answerResult == null
                ? AnswerResult.empty("", List.of())
                : answerResult;
        Instant finishedAt = Instant.now();
        Map<String, Object> attributes = orderedMap(
                "requestId", session.requestId(),
                "conversationId", session.conversationId(),
                "status", safeAnswerResult.status(),
                "partial", safeAnswerResult.partial(),
                "empty", safeAnswerResult.empty(),
                "snapshotMissAffected", safeAnswerResult.snapshotMissAffected(),
                "evidenceCount", safeAnswerResult.evidenceSummaries().size());
        appendStage(session, "final-answer", safeAnswerResult.status(), abbreviate(safeAnswerResult.answerText()), startedAt, finishedAt, attributes);
    }

    /**
     * Records an exceptional stage failure as trace data.
     *
     * @param session current trace session
     * @param stageName stage that failed
     * @param startedAt stage start time
     * @param throwable failure thrown by the stage
     */
    @Override
    public void recordFailure(RagTraceSession session, String stageName, Instant startedAt, Throwable throwable) {
        Instant finishedAt = Instant.now();
        Map<String, Object> attributes = orderedMap(
                "requestId", session.requestId(),
                "conversationId", session.conversationId(),
                "status", "FAILED",
                "errorType", throwable == null ? "" : throwable.getClass().getSimpleName(),
                "error", throwable == null ? "" : abbreviate(throwable.getMessage()));
        appendStage(session, stageName, "FAILED", throwable == null ? "" : abbreviate(throwable.getMessage()), startedAt, finishedAt, attributes);
    }

    /**
     * Finalizes the trace result and attempts to persist it.
     *
     * @param session current trace session
     * @param overallStatus overall request status
     * @param snapshotMissOccurred whether a snapshot miss affected the request
     * @return finalized trace result
     */
    @Override
    public RagTraceResult finish(RagTraceSession session, String overallStatus, boolean snapshotMissOccurred) {
        RagTraceResult result = new RagTraceResult(
                session.root().traceId(),
                session.requestId(),
                session.conversationId(),
                session.root().scene(),
                overallStatus,
                snapshotMissOccurred,
                session.root(),
                session.stageResults(),
                session.mcpToolSummaries());
        try {
            // Persistence is best-effort here because the query response should still succeed even if trace
            // storage is temporarily unavailable.
            ragTraceRecordService.persist(result);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist rag trace result, traceId={}", result.traceId(), exception);
        }
        return result;
    }

    /**
     * Clears the thread-local trace context after the request finishes.
     */
    @Override
    public void clear() {
        ragTraceRecorder.clear();
    }

    private void appendStage(RagTraceSession session,
                             String stageName,
                             String status,
                             String summary,
                             Instant startedAt,
                             Instant finishedAt,
                             Map<String, Object> attributes) {
        Map<String, Object> nodeAttributes = enrichNodeAttributes(attributes, status, summary);
        ragTraceRecorder.appendNode(session.root(), stageName, startedAt, finishedAt, nodeAttributes);
        session.stageResults().add(new RagTraceStageResult(stageName, status, summary, startedAt, finishedAt, nodeAttributes));
    }

    private RagTraceToolSummary toToolSummary(McpExecutionRecord execution) {
        return new RagTraceToolSummary(
                execution.toolId(),
                execution.status(),
                firstNonBlank(execution.message(), execution.error()),
                execution.snapshotMiss());
    }

    private Map<String, Object> toToolSummaryMap(McpExecutionRecord execution) {
        return orderedMap(
                "toolId", execution.toolId(),
                "status", execution.status(),
                "message", firstNonBlank(execution.message(), execution.error()),
                "snapshotMiss", execution.snapshotMiss());
    }

    private Map<String, Object> orderedMap(Object... entries) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            attributes.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(attributes);
    }

    private String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 180) {
            return safeValue;
        }
        return safeValue.substring(0, 177) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private Map<String, Object> enrichNodeAttributes(Map<String, Object> attributes, String status, String summary) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(attributes == null ? Map.of() : attributes);
        values.put("status", firstNonBlank(status, stringValue(values.get("status"))));
        values.put("summary", firstNonBlank(summary, stringValue(values.get("summary"))));
        return Map.copyOf(values);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

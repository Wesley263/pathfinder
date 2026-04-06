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
 * 说明。
 *
 * 说明。
 * 并在结束后交给持久化层处理。
 * 说明。
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
     * 说明。
     *
     * @param requestId 当前查询请求标识
     * @param conversationId 与查询关联的会话标识
     * @return 返回结果。
     */
    @Override
    public RagTraceSession startQueryTrace(String requestId, String conversationId) {
        TraceRoot root = ragTraceRecorder.startRoot(SCENE);
        return new RagTraceSession(requestId, conversationId, root);
    }

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 阶段开始时间
     * @param stageOneResult 参数说明。
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
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 阶段开始时间
     * @param retrievalResult 待汇总的检索结果
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
                // 工具执行作为独立内部节点记录，
                // 说明。
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
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 阶段开始时间
     * @param answerResult 待汇总的最终回答输出
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
     * 说明。
     *
     * @param session 参数说明。
     * @param stageName 失败阶段名
     * @param startedAt 阶段开始时间
     * @param throwable 阶段抛出的异常
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
     * 说明。
     *
     * @param session 参数说明。
     * @param overallStatus 请求整体状态
     * @param snapshotMissOccurred 快照缺失是否影响请求
     * @return 返回结果。
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
            // 这里采用“尽力持久化”策略：
            // 说明。
            ragTraceRecordService.persist(result);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist rag trace result, traceId={}", result.traceId(), exception);
        }
        return result;
    }

    /**
     * 说明。
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

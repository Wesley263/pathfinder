package com.flightpathfinder.rag.service.impl;

import com.flightpathfinder.rag.core.answer.AnswerEvidenceSummary;
import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.answer.FinalAnswerService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.memory.ConversationMemoryService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryWriteRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagPipeline;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalService;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import com.flightpathfinder.rag.core.trace.RagTraceService;
import com.flightpathfinder.rag.core.trace.RagTraceSession;
import com.flightpathfinder.rag.service.RagStreamService;
import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEvent;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Default streaming RAG application orchestrator.
 *
 * <p>This service reuses the same mainline stages as the synchronous query path, but it emits
 * stage and answer progress as SSE-friendly events. Memory and trace still stay in the
 * orchestration layer rather than leaking into controllers.</p>
 */
@Service
public class DefaultRagStreamService implements RagStreamService {

    private static final int ANSWER_CHUNK_SIZE = 160;

    private final StageOneRagPipeline stageOneRagPipeline;
    private final RetrievalService retrievalService;
    private final FinalAnswerService finalAnswerService;
    private final ConversationMemoryService conversationMemoryService;
    private final RagTraceService ragTraceService;
    private final TaskExecutor taskExecutor;

    public DefaultRagStreamService(StageOneRagPipeline stageOneRagPipeline,
                                   RetrievalService retrievalService,
                                   FinalAnswerService finalAnswerService,
                                   ConversationMemoryService conversationMemoryService,
                                   RagTraceService ragTraceService,
                                   ObjectProvider<TaskExecutor> taskExecutorProvider) {
        this.stageOneRagPipeline = stageOneRagPipeline;
        this.retrievalService = retrievalService;
        this.finalAnswerService = finalAnswerService;
        this.conversationMemoryService = conversationMemoryService;
        this.ragTraceService = ragTraceService;
        this.taskExecutor = taskExecutorProvider.getIfAvailable(() -> {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rag-stream-");
            executor.setDaemon(true);
            return executor;
        });
    }

    /**
     * Starts the streaming RAG mainline asynchronously and writes progress events through the
     * provided event writer.
     *
     * @param command request command derived from the streaming API
     * @param eventWriter sink for stage, chunk, and completion events
     */
    @Override
    public void stream(RagStreamCommand command, RagStreamEventWriter eventWriter) {
        RagStreamCommand safeCommand = command == null
                ? new RagStreamCommand("", "", "")
                : command;
        RagStreamEventWriter safeWriter = eventWriter == null ? event -> { } : eventWriter;
        taskExecutor.execute(() -> runStream(safeCommand, safeWriter));
    }

    private void runStream(RagStreamCommand command, RagStreamEventWriter eventWriter) {
        AtomicLong sequence = new AtomicLong(1L);
        RagTraceSession traceSession = ragTraceService.startQueryTrace(command.requestId(), command.conversationId());
        String traceId = traceSession.root().traceId();
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // Streaming keeps the same memory load boundary as the synchronous path so
            // follow-up handling stays consistent across both entry points.
            ConversationMemoryContext memoryContext = conversationMemoryService.loadContext(command.conversationId());

            emit(eventWriter, sequence, "stage_one_started", "stage_one", command, traceId, "STARTED", null, null, "",
                    orderedMap(
                            "question", command.question(),
                            "memoryTurnCount", memoryContext.recentTurns().size(),
                            "memorySummaryPresent", !memoryContext.summary().summaryText().isBlank()));

            currentStage = "stage-one";
            currentStageStartedAt = Instant.now();
            StageOneRagResult stageOneResult = stageOneRagPipeline.run(new StageOneRagRequest(
                    command.question(),
                    command.conversationId(),
                    command.requestId(),
                    memoryContext));
            ragTraceService.recordStageOne(traceSession, currentStageStartedAt, stageOneResult);
            emit(eventWriter, sequence, "stage_one_completed", "stage_one", command, traceId, stageOneStatus(stageOneResult), null, null, "",
                    orderedMap(
                            "rewrittenQuestion", stageOneResult.rewriteResult().rewrittenQuestion(),
                            "subQuestions", stageOneResult.rewriteResult().subQuestions(),
                            "kbIntentIds", stageOneResult.intentSplitResult().kbIntents().stream().map(intent -> intent.intentId()).toList(),
                            "mcpIntentIds", stageOneResult.intentSplitResult().mcpIntents().stream().map(intent -> intent.intentId()).toList(),
                            "systemIntentIds", stageOneResult.intentSplitResult().systemIntents().stream().map(intent -> intent.intentId()).toList(),
                            "memoryTurnCount", stageOneResult.memoryContext().recentTurns().size(),
                            "memorySummary", stageOneResult.memoryContext().summary().summaryText()));

            emit(eventWriter, sequence, "retrieval_started", "retrieval", command, traceId, "STARTED", null, null, "",
                    orderedMap(
                            "rewrittenQuestion", stageOneResult.rewriteResult().rewrittenQuestion(),
                            "mcpIntentCount", stageOneResult.intentSplitResult().mcpIntents().size(),
                            "kbIntentCount", stageOneResult.intentSplitResult().kbIntents().size()));

            currentStage = "retrieval";
            currentStageStartedAt = Instant.now();
            RetrievalResult retrievalResult = retrievalService.retrieve(stageOneResult);
            ragTraceService.recordRetrieval(traceSession, currentStageStartedAt, retrievalResult);
            emit(eventWriter, sequence, "retrieval_completed", "retrieval", command, traceId, retrievalResult.status(),
                    retrievalPartial(retrievalResult),
                    retrievalResult.hasSnapshotMiss(),
                    "",
                    orderedMap(
                            "summary", retrievalResult.summary(),
                            "kbStatus", retrievalResult.kbContext().status(),
                            "kbItemCount", retrievalResult.kbContext().items().size(),
                            "mcpStatus", retrievalResult.mcpContext().status(),
                            "mcpExecutionCount", retrievalResult.mcpContext().executions().size(),
                            "snapshotMiss", retrievalResult.hasSnapshotMiss(),
                            "toolSummaries", retrievalResult.mcpContext().executions().stream()
                                    .map(this::toToolSummary)
                                    .toList()));

            emit(eventWriter, sequence, "final_answer_started", "final_answer", command, traceId, "STARTED", null,
                    retrievalResult.hasSnapshotMiss(), "",
                    orderedMap(
                            "mode", "CHUNKED",
                            "retrievalStatus", retrievalResult.status(),
                            "snapshotMiss", retrievalResult.hasSnapshotMiss()));

            currentStage = "final-answer";
            currentStageStartedAt = Instant.now();
            AnswerResult answerResult = finalAnswerService.answer(retrievalResult);
            ragTraceService.recordFinalAnswer(traceSession, currentStageStartedAt, answerResult);

            // Memory write is still owned here, not by the SSE controller, so both sync and
            // stream paths share the same persistence semantics.
            conversationMemoryService.appendTurn(new ConversationMemoryWriteRequest(
                    command.conversationId(),
                    command.requestId(),
                    command.question(),
                    stageOneResult.rewriteResult().rewrittenQuestion(),
                    answerResult.answerText(),
                    answerResult.status(),
                    Instant.now()));

            RagTraceResult traceResult = ragTraceService.finish(
                    traceSession,
                    answerResult.status(),
                    answerResult.snapshotMissAffected());

            List<String> chunks = splitIntoChunks(answerResult.answerText());
            for (int index = 0; index < chunks.size(); index++) {
                // The current stream is chunked rather than token-native, but the event shape
                // already matches the phase boundaries needed by later true token streaming.
                emit(eventWriter, sequence, "final_answer_chunk", "final_answer", command, traceId, "STREAMING",
                        answerResult.partial(),
                        answerResult.snapshotMissAffected(),
                        "",
                        orderedMap(
                                "chunkIndex", index,
                                "chunkCount", chunks.size(),
                                "chunk", chunks.get(index)));
            }

            emit(eventWriter, sequence, "final_answer_completed", "final_answer", command, traceId, answerResult.status(),
                    answerResult.partial(),
                    answerResult.snapshotMissAffected(),
                    "",
                    orderedMap(
                            "answerText", answerResult.answerText(),
                            "empty", answerResult.empty(),
                            "evidenceSummaries", answerResult.evidenceSummaries().stream().map(this::toEvidenceSummary).toList(),
                            "stageOneCompleted", isStageOneCompleted(stageOneResult),
                            "retrievalStatus", retrievalResult.status(),
                            "traceStatus", traceResult.overallStatus()));
        } catch (RuntimeException exception) {
            ragTraceService.recordFailure(traceSession, currentStage, currentStageStartedAt, exception);
            ragTraceService.finish(traceSession, "FAILED", false);
            emitSafely(eventWriter, new RagStreamEvent(
                    sequence.getAndIncrement(),
                    "error",
                    currentStageName(currentStage),
                    command.requestId(),
                    command.conversationId(),
                    traceId,
                    Instant.now().toString(),
                    "FAILED",
                    null,
                    null,
                    firstNonBlank(exception.getMessage(), exception.getClass().getSimpleName()),
                    orderedMap(
                            "errorType", exception.getClass().getSimpleName(),
                            "message", firstNonBlank(exception.getMessage(), exception.getClass().getSimpleName()))));
        } finally {
            ragTraceService.clear();
            eventWriter.complete();
        }
    }

    private void emit(RagStreamEventWriter eventWriter,
                      AtomicLong sequence,
                      String event,
                      String stage,
                      RagStreamCommand command,
                      String traceId,
                      String status,
                      Boolean partial,
                      Boolean snapshotMissAffected,
                      String error,
                      Map<String, Object> data) {
        emitSafely(eventWriter, new RagStreamEvent(
                sequence.getAndIncrement(),
                event,
                stage,
                command.requestId(),
                command.conversationId(),
                traceId,
                Instant.now().toString(),
                status,
                partial,
                snapshotMissAffected,
                error,
                data));
    }

    private void emitSafely(RagStreamEventWriter eventWriter, RagStreamEvent event) {
        eventWriter.emit(event);
    }

    private boolean retrievalPartial(RetrievalResult retrievalResult) {
        return retrievalResult.hasSnapshotMiss()
                || retrievalResult.kbContext().hasError()
                || retrievalResult.mcpContext().hasErrors();
    }

    private String stageOneStatus(StageOneRagResult stageOneResult) {
        return isStageOneCompleted(stageOneResult) ? "SUCCESS" : "EMPTY";
    }

    private boolean isStageOneCompleted(StageOneRagResult stageOneResult) {
        return !stageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                || stageOneResult.rewriteResult().subQuestions().stream().anyMatch(question -> question != null && !question.isBlank())
                || !stageOneResult.intentSplitResult().kbIntents().isEmpty()
                || !stageOneResult.intentSplitResult().mcpIntents().isEmpty()
                || !stageOneResult.intentSplitResult().systemIntents().isEmpty();
    }

    private List<String> splitIntoChunks(String answerText) {
        String safeText = answerText == null ? "" : answerText.trim();
        if (safeText.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int index = 0;
        while (index < safeText.length()) {
            int end = Math.min(safeText.length(), index + ANSWER_CHUNK_SIZE);
            if (end < safeText.length()) {
                int whitespace = safeText.lastIndexOf(' ', end);
                int newline = safeText.lastIndexOf('\n', end);
                int breakpoint = Math.max(whitespace, newline);
                if (breakpoint > index + (ANSWER_CHUNK_SIZE / 2)) {
                    end = breakpoint;
                }
            }
            chunks.add(safeText.substring(index, end).trim());
            index = end;
            while (index < safeText.length() && Character.isWhitespace(safeText.charAt(index))) {
                index++;
            }
        }
        return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
    }

    private Map<String, Object> toToolSummary(McpExecutionRecord execution) {
        return orderedMap(
                "toolId", execution.toolId(),
                "status", execution.status(),
                "message", firstNonBlank(execution.message(), execution.error()),
                "snapshotMiss", execution.snapshotMiss());
    }

    private Map<String, Object> toEvidenceSummary(AnswerEvidenceSummary evidenceSummary) {
        return orderedMap(
                "type", evidenceSummary.type(),
                "source", evidenceSummary.source(),
                "label", evidenceSummary.label(),
                "status", evidenceSummary.status(),
                "snippet", evidenceSummary.snippet());
    }

    private Map<String, Object> orderedMap(Object... entries) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(values);
    }

    private String currentStageName(String stageName) {
        return switch (stageName) {
            case "stage-one" -> "stage_one";
            case "retrieval" -> "retrieval";
            case "final-answer" -> "final_answer";
            default -> "error";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

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
 * RAG 流式响应编排服务默认实现。
 *
 * 负责驱动 stage one、retrieval、final answer 三阶段并输出事件流。
 */
@Service
public class DefaultRagStreamService implements RagStreamService {

    /** 最终回答分块大小（字符数）。 */
    private static final int ANSWER_CHUNK_SIZE = 160;

    /** 第一阶段编排器。 */
    private final StageOneRagPipeline stageOneRagPipeline;
    /** 检索阶段编排器。 */
    private final RetrievalService retrievalService;
    /** 最终回答阶段服务。 */
    private final FinalAnswerService finalAnswerService;
    /** 会话记忆服务。 */
    private final ConversationMemoryService conversationMemoryService;
    /** 追踪生命周期服务。 */
    private final RagTraceService ragTraceService;
    /** 异步任务执行器，用于在后台推送事件流。 */
    private final TaskExecutor taskExecutor;

    /**
     * 构造流式请求编排器。
     *
     * @param stageOneRagPipeline 第一阶段编排器
    * @param retrievalService 检索阶段编排器
    * @param finalAnswerService 最终回答阶段服务
     * @param conversationMemoryService 会话记忆服务
    * @param ragTraceService 追踪生命周期服务
     * @param taskExecutorProvider 可选任务执行器提供者
     */
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
        * 提交一次流式请求。
     *
        * @param command 流式请求命令
        * @param eventWriter 事件写出器
     */
    @Override
    public void stream(RagStreamCommand command, RagStreamEventWriter eventWriter) {
        RagStreamCommand safeCommand = command == null
                ? new RagStreamCommand("", "", "")
                : command;
        RagStreamEventWriter safeWriter = eventWriter == null ? event -> { } : eventWriter;
        taskExecutor.execute(() -> runStream(safeCommand, safeWriter));
    }

    /**
     * 在后台线程执行完整流式主链。
     *
     * @param command 流式命令
     * @param eventWriter 事件输出器
     */
    private void runStream(RagStreamCommand command, RagStreamEventWriter eventWriter) {
        AtomicLong sequence = new AtomicLong(1L);
        RagTraceSession traceSession = ragTraceService.startQueryTrace(command.requestId(), command.conversationId());
        String traceId = traceSession.root().traceId();
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // 先加载会话记忆，保证 stage one 的上下文完整。
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

                // 主链成功后写回本轮问答到会话记忆。
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
                // 以 chunk 事件输出回答正文，兼容前端分段渲染。
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

    /**
     * 发送一条标准化流式事件。
     *
     * @param eventWriter 事件输出器
     * @param sequence 序号计数器
     * @param event 事件名
     * @param stage 阶段名
     * @param command 当前命令
    * @param traceId trace 标识
     * @param status 事件状态
     * @param partial 是否部分结果
    * @param snapshotMissAffected 是否受快照缺失影响
     * @param error 错误信息
     * @param data 事件数据体
     */
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

    /**
     * 安全发送事件。
     *
     * @param eventWriter 事件输出器
     * @param event 事件对象
     */
    private void emitSafely(RagStreamEventWriter eventWriter, RagStreamEvent event) {
        eventWriter.emit(event);
    }

    /**
        * 判断检索阶段是否为部分结果。
     *
        * @param retrievalResult 检索阶段结果
        * @return 是否部分结果
     */
    private boolean retrievalPartial(RetrievalResult retrievalResult) {
        return retrievalResult.hasSnapshotMiss()
                || retrievalResult.kbContext().hasError()
                || retrievalResult.mcpContext().hasErrors();
    }

    /**
        * 计算 stage one 事件状态。
     *
     * @param stageOneResult 第一阶段结果
        * @return 事件状态
     */
    private String stageOneStatus(StageOneRagResult stageOneResult) {
        return isStageOneCompleted(stageOneResult) ? "SUCCESS" : "EMPTY";
    }

    /**
     * 判断第一阶段是否产出有效结果。
     *
     * @param stageOneResult 第一阶段结果
    * @return 是否产出有效结果
     */
    private boolean isStageOneCompleted(StageOneRagResult stageOneResult) {
        return !stageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                || stageOneResult.rewriteResult().subQuestions().stream().anyMatch(question -> question != null && !question.isBlank())
                || !stageOneResult.intentSplitResult().kbIntents().isEmpty()
                || !stageOneResult.intentSplitResult().mcpIntents().isEmpty()
                || !stageOneResult.intentSplitResult().systemIntents().isEmpty();
    }

    /**
        * 将回答文本按块切分。
     *
     * @param answerText 原始回答文本
        * @return 文本分块列表
     */
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

    /**
        * 转换工具执行记录为事件摘要对象。
     *
        * @param execution 工具执行记录
     * @return 摘要对象
     */
    private Map<String, Object> toToolSummary(McpExecutionRecord execution) {
        return orderedMap(
                "toolId", execution.toolId(),
                "status", execution.status(),
                "message", firstNonBlank(execution.message(), execution.error()),
                "snapshotMiss", execution.snapshotMiss());
    }

    /**
     * 转换单条回答证据摘要，供事件输出。
     *
     * @param evidenceSummary 回答证据摘要
     * @return 摘要对象
     */
    private Map<String, Object> toEvidenceSummary(AnswerEvidenceSummary evidenceSummary) {
        return orderedMap(
                "type", evidenceSummary.type(),
                "source", evidenceSummary.source(),
                "label", evidenceSummary.label(),
                "status", evidenceSummary.status(),
                "snippet", evidenceSummary.snippet());
    }

    /**
        * 构造保持插入顺序的只读 Map。
     *
     * @param entries 键值交替参数
        * @return 有序只读 Map
     */
    private Map<String, Object> orderedMap(Object... entries) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(values);
    }

    /**
     * 将内部阶段名映射为对外事件阶段名。
     *
     * @param stageName 内部阶段名
     * @return 事件阶段名
     */
    private String currentStageName(String stageName) {
        return switch (stageName) {
            case "stage-one" -> "stage_one";
            case "retrieval" -> "retrieval";
            case "final-answer" -> "final_answer";
            default -> "error";
        };
    }

    /**
     * 返回首个非空白文本。
     *
     * @param values 候选文本
     * @return 首个非空白值；若都为空则返回空字符串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}


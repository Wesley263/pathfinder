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
 * 同步 RAG 请求的默认应用编排器。
 *
 * <p>它负责一次同步请求的全生命周期：memory load、stage one、retrieval、
 * 最终回答、记忆写入与追踪收口。这样 controller 可以保持薄层，
 * 各阶段服务也不会被迫承担跨阶段编排职责。</p>
 */
@Service
public class DefaultRagQueryService implements RagQueryService {

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

    /**
     * 构造同步请求编排器。
     *
     * @param stageOneRagPipeline 第一阶段编排器
     * @param retrievalService retrieval 阶段服务
     * @param finalAnswerService final answer 阶段服务
     * @param conversationMemoryService 会话记忆服务
     * @param ragTraceService trace 生命周期服务
     */
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
     * 执行同步 RAG 主链并返回完整查询结果。
     *
     * @param command 由 user-facing API 转换而来的查询命令
     * @return 当前请求对应的 stage one、retrieval、answer 与 trace 结果
     */
    @Override
    public RagQueryResult query(RagQueryCommand command) {
        RagQueryCommand safeCommand = command == null
                ? new RagQueryCommand("", "", "")
                : command;
        // 追踪在任何阶段前启动，确保成功路径和早失败路径都能共享同一个请求级 traceId。
        RagTraceSession traceSession = ragTraceService.startQueryTrace(
                safeCommand.requestId(),
                safeCommand.conversationId());
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // 记忆在第一阶段前加载，保证追问改写与路由能使用最新上下文，同时不把 memory 逻辑下沉到阶段服务。
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

            // 记忆写入放在最终回答之后，后续轮次才能看到本次请求真实产出的改写问题和回答状态。
            conversationMemoryService.appendTurn(new ConversationMemoryWriteRequest(
                    safeCommand.conversationId(),
                    safeCommand.requestId(),
                    safeCommand.question(),
                    stageOneResult.rewriteResult().rewrittenQuestion(),
                    answerResult.answerText(),
                    answerResult.status(),
                    Instant.now()));

            // 追踪在记忆写入之后收口，持久化结果才完整覆盖一次同步请求生命周期。
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
            // 最终在 finally 中清理追踪上下文，避免异常路径把上下文泄漏到下一次请求。
            ragTraceService.clear();
        }
    }
}



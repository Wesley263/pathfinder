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
 * Rag 同步问答应用服务默认实现。
 *
 * 负责编排阶段一分析、检索、最终回答、会话记忆写入与追踪收口。
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
        * @param retrievalService 检索服务
        * @param finalAnswerService 最终回答服务
     * @param conversationMemoryService 会话记忆服务
        * @param ragTraceService 追踪生命周期服务
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
         * 执行一次同步 Rag 查询。
     *
         * @param command 查询命令
         * @return 聚合后的阶段结果、回答结果与追踪结果
     */
    @Override
    public RagQueryResult query(RagQueryCommand command) {
        RagQueryCommand safeCommand = command == null
                ? new RagQueryCommand("", "", "")
                : command;
        // 先创建追踪会话，确保后续每个阶段都可被记录并持久化。
        RagTraceSession traceSession = ragTraceService.startQueryTrace(
                safeCommand.requestId(),
                safeCommand.conversationId());
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // 先加载历史记忆，为改写与路由阶段提供上下文。
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
            // 无论成功失败都清理线程追踪上下文，避免请求间污染。
            ragTraceService.clear();
        }
    }
}




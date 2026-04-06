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
 * 说明。
 *
 * 说明。
 * 说明。
 * 说明。
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
     * @param retrievalService 参数说明。
     * @param finalAnswerService 参数说明。
     * @param conversationMemoryService 会话记忆服务
     * @param ragTraceService 参数说明。
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
     * 说明。
     *
     * @param command 参数说明。
     * @return 返回结果。
     */
    @Override
    public RagQueryResult query(RagQueryCommand command) {
        RagQueryCommand safeCommand = command == null
                ? new RagQueryCommand("", "", "")
                : command;
        // 说明。
        RagTraceSession traceSession = ragTraceService.startQueryTrace(
                safeCommand.requestId(),
                safeCommand.conversationId());
        String currentStage = "query";
        Instant currentStageStartedAt = Instant.now();
        try {
            // 说明。
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
            // 说明。
            ragTraceService.clear();
        }
    }
}



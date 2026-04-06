package com.flightpathfinder.rag.service.model;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import java.util.List;

/**
 * 同步查询总结果。
 *
 * 聚合阶段一、检索、回答与追踪结果，作为同步链路统一返回模型。
 *
 * @param stageOneResult 第一阶段结果
 * @param retrievalResult 检索结果
 * @param answerResult 最终回答结果
 * @param traceResult 追踪结果
 */
public record RagQueryResult(
        StageOneRagResult stageOneResult,
        RetrievalResult retrievalResult,
        AnswerResult answerResult,
        RagTraceResult traceResult) {

    /**
     * 归一化同步查询结果。
     */
    public RagQueryResult {
        stageOneResult = stageOneResult == null ? new StageOneRagResult(null, null, null, null) : stageOneResult;
        retrievalResult = retrievalResult == null ? new RetrievalResult(null, null, null, null, null) : retrievalResult;
        answerResult = answerResult == null
                ? com.flightpathfinder.rag.core.answer.AnswerResult.empty("", java.util.List.of())
                : answerResult;
        traceResult = traceResult == null
                ? new RagTraceResult("", "", "", "", "NOT_AVAILABLE", false, null, List.of(), List.of())
                : traceResult;
    }

    /**
     * 判断第一阶段是否产生了有效结果。
     *
        * @return 产生改写或任一路意图时返回 true
     */
    public boolean stageOneCompleted() {
        return !stageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                || stageOneResult.rewriteResult().subQuestions().stream().anyMatch(question -> question != null && !question.isBlank())
                || !stageOneResult.intentSplitResult().kbIntents().isEmpty()
                || !stageOneResult.intentSplitResult().mcpIntents().isEmpty()
                || !stageOneResult.intentSplitResult().systemIntents().isEmpty();
    }
}


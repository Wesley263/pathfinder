package com.flightpathfinder.rag.service.model;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import java.util.List;

/**
 * 同步查询总结果。
 *
 * <p>它把 stage one、retrieval、answer 和 trace 统一收口为一个应用层结果对象，
 * 供同步 controller 组装最终响应。</p>
 *
 * @param stageOneResult 第一阶段结果
 * @param retrievalResult retrieval 阶段结果
 * @param answerResult 最终回答结果
 * @param traceResult 本次请求的 trace 结果
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
     * @return 若改写结果或任一路意图非空，则返回 true
     */
    public boolean stageOneCompleted() {
        return !stageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                || stageOneResult.rewriteResult().subQuestions().stream().anyMatch(question -> question != null && !question.isBlank())
                || !stageOneResult.intentSplitResult().kbIntents().isEmpty()
                || !stageOneResult.intentSplitResult().mcpIntents().isEmpty()
                || !stageOneResult.intentSplitResult().systemIntents().isEmpty();
    }
}

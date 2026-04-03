package com.flightpathfinder.rag.service.model;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.trace.RagTraceResult;
import java.util.List;

public record RagQueryResult(
        StageOneRagResult stageOneResult,
        RetrievalResult retrievalResult,
        AnswerResult answerResult,
        RagTraceResult traceResult) {

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

    public boolean stageOneCompleted() {
        return !stageOneResult.rewriteResult().rewrittenQuestion().isBlank()
                || stageOneResult.rewriteResult().subQuestions().stream().anyMatch(question -> question != null && !question.isBlank())
                || !stageOneResult.intentSplitResult().kbIntents().isEmpty()
                || !stageOneResult.intentSplitResult().mcpIntents().isEmpty()
                || !stageOneResult.intentSplitResult().systemIntents().isEmpty();
    }
}

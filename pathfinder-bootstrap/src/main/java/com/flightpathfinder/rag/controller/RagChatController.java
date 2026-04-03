package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.context.RequestIdHolder;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.request.RagChatRequest;
import com.flightpathfinder.rag.controller.vo.RagAnswerEvidenceVO;
import com.flightpathfinder.rag.controller.vo.RagAuditVO;
import com.flightpathfinder.rag.controller.vo.RagChatResponseVO;
import com.flightpathfinder.rag.controller.vo.RagTraceStageVO;
import com.flightpathfinder.rag.controller.vo.RagTraceToolVO;
import com.flightpathfinder.rag.service.RagQueryService;
import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous user-facing RAG controller.
 *
 * <p>This controller only adapts HTTP to the synchronous query service. Memory loading,
 * trace lifecycle, retrieval orchestration, and audit assembly all stay in the service layer
 * so the web layer does not become a hidden orchestrator.</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagChatController {

    private final RagQueryService ragQueryService;

    public RagChatController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    /**
     * Runs the synchronous RAG mainline and returns one complete response payload.
     *
     * @param request chat request from the user-facing API
     * @return final answer plus audit data derived from the orchestrated mainline
     */
    @PostMapping("/chat")
    public Result<RagChatResponseVO> chat(@Valid @RequestBody RagChatRequest request) {
        RagQueryResult queryResult = ragQueryService.query(new RagQueryCommand(
                request.question(),
                request.conversationId(),
                RequestIdHolder.getOrCreate()));
        return Results.success(toResponse(request.question(), queryResult));
    }

    private RagChatResponseVO toResponse(String question, RagQueryResult queryResult) {
        // Audit and trace summaries are flattened here for response readability, but the
        // underlying lifecycle is still owned by the orchestration service.
        return new RagChatResponseVO(
                question == null ? "" : question.trim(),
                queryResult.stageOneResult().rewriteResult().rewrittenQuestion(),
                queryResult.stageOneResult().rewriteResult().subQuestions(),
                queryResult.answerResult().answerText(),
                queryResult.answerResult().partial(),
                queryResult.answerResult().snapshotMissAffected(),
                new RagAuditVO(
                        queryResult.stageOneCompleted(),
                        queryResult.retrievalResult().status(),
                        queryResult.answerResult().status(),
                        queryResult.answerResult().snapshotMissAffected(),
                        queryResult.traceResult().requestId(),
                        queryResult.traceResult().conversationId(),
                        queryResult.traceResult().traceId(),
                        queryResult.traceResult().overallStatus(),
                        queryResult.traceResult().stages().stream()
                                .map(stage -> new RagTraceStageVO(
                                        stage.stageName(),
                                        stage.status(),
                                        stage.summary()))
                                .toList(),
                        queryResult.traceResult().mcpToolSummaries().stream()
                                .map(tool -> new RagTraceToolVO(
                                        tool.toolId(),
                                        tool.status(),
                                        tool.message(),
                                        tool.snapshotMiss()))
                                .toList()),
                queryResult.stageOneResult().intentSplitResult().kbIntents().stream()
                        .map(intent -> intent.intentId())
                        .toList(),
                queryResult.stageOneResult().intentSplitResult().mcpIntents().stream()
                        .map(intent -> intent.intentId())
                        .toList(),
                queryResult.stageOneResult().intentSplitResult().systemIntents().stream()
                        .map(intent -> intent.intentId())
                        .toList(),
                queryResult.answerResult().evidenceSummaries().stream()
                        .map(evidence -> new RagAnswerEvidenceVO(
                                evidence.type(),
                                evidence.source(),
                                evidence.label(),
                                evidence.status(),
                                evidence.snippet()))
                        .toList());
    }
}

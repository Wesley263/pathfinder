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
 * 同步 user-facing RAG 控制器。
 *
 * <p>它只负责把 HTTP 请求适配成同步查询服务调用。memory 读取、trace 生命周期、
 * 检索编排和审计组装都留在 service 层，避免 controller 变成隐藏编排器。</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagChatController {

    /** 同步主链应用服务。 */
    private final RagQueryService ragQueryService;

    /**
     * 构造同步聊天控制器。
     *
     * @param ragQueryService 同步主链应用服务
     */
    public RagChatController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    /**
     * 执行同步 RAG 主链并返回完整响应。
     *
     * @param request 用户面对话请求
     * @return 包含回答正文和审计信息的统一响应
     */
    @PostMapping("/chat")
    public Result<RagChatResponseVO> chat(@Valid @RequestBody RagChatRequest request) {
        RagQueryResult queryResult = ragQueryService.query(new RagQueryCommand(
                request.question(),
                request.conversationId(),
                RequestIdHolder.getOrCreate()));
        return Results.success(toResponse(request.question(), queryResult));
    }

    /**
     * 把应用层查询结果转换为接口响应对象。
     *
     * @param question 原始问题
     * @param queryResult 应用层总结果
     * @return 面向接口返回的响应对象
     */
    private RagChatResponseVO toResponse(String question, RagQueryResult queryResult) {
        // 这里只做展示层扁平化，真正的 memory / trace / retrieval 生命周期仍归应用编排层所有。
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


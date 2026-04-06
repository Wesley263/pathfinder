package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import org.springframework.stereotype.Service;

/**
 * 检索阶段的默认编排器。
 *
 * 协调 KB 分支与 MCP 分支，并将两者汇总为统一 RetrievalResult。
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    /** 知识库分支检索器。 */
    private final KnowledgeRetriever knowledgeRetriever;
    /** MCP 分支执行器。 */
    private final McpContextExecutor mcpContextExecutor;

    /**
     * 构造检索阶段编排器。
     *
     * @param knowledgeRetriever 知识库分支检索器
     * @param mcpContextExecutor MCP 分支执行器
     */
    public DefaultRetrievalService(KnowledgeRetriever knowledgeRetriever, McpContextExecutor mcpContextExecutor) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.mcpContextExecutor = mcpContextExecutor;
    }

    /**
     * 执行检索阶段编排。
     *
     * @param stageOneRagResult 第一阶段输出
     * @return 检索总结果
     */
    @Override
    public RetrievalResult retrieve(StageOneRagResult stageOneRagResult) {
        StageOneRagResult safeStageOneResult = stageOneRagResult == null
                ? new StageOneRagResult(null, null, null, null)
                : stageOneRagResult;

        // 两条分支基于同一份 stage one 输出并行语义执行。
        KbContext kbContext = knowledgeRetriever.retrieve(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());
        McpContext mcpContext = mcpContextExecutor.execute(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());

        // 聚合阶段统一输出状态与摘要，减少上游分支判断复杂度。
        return new RetrievalResult(
                resolveStatus(kbContext, mcpContext),
                buildSummary(kbContext, mcpContext),
                safeStageOneResult,
                kbContext,
                mcpContext);
    }

    /**
         * 依据 KB/MCP 两条分支结果推导检索总状态。
     *
         * @param kbContext KB 检索上下文
         * @param mcpContext MCP 执行上下文
         * @return 聚合状态
     */
    private String resolveStatus(KbContext kbContext, McpContext mcpContext) {
        boolean kbError = kbContext.hasError();
        boolean mcpSnapshotMiss = mcpContext.hasSnapshotMiss();
        boolean mcpError = mcpContext.hasErrors();
        boolean mcpPartialSuccess = "PARTIAL_SUCCESS".equals(mcpContext.status());
        boolean mcpDataNotFound = "DATA_NOT_FOUND".equals(mcpContext.status());
        boolean hasSuccessfulKb = !kbContext.hasError() && !kbContext.empty();
        boolean hasMcpExecutions = !mcpContext.executions().isEmpty();
        boolean hasSuccessfulMcp = hasMcpExecutions && !mcpError;

        if (mcpSnapshotMiss && !kbError) {
            return "SNAPSHOT_MISS";
        }
        if (kbError && mcpError) {
            return "FAILED";
        }
        if (kbError || mcpError) {
            return hasSuccessfulKb || hasSuccessfulMcp ? "PARTIAL_FAILURE" : "FAILED";
        }
        if (mcpDataNotFound) {
            return hasSuccessfulKb ? "PARTIAL_SUCCESS" : "DATA_NOT_FOUND";
        }
        if (mcpPartialSuccess) {
            return "PARTIAL_SUCCESS";
        }
        if (kbContext.empty() && !hasMcpExecutions) {
            return "EMPTY";
        }
        return "SUCCESS";
    }

    /**
        * 构建检索摘要文本。
     *
        * @param kbContext KB 检索上下文
        * @param mcpContext MCP 执行上下文
     * @return 简洁摘要文本
     */
    private String buildSummary(KbContext kbContext, McpContext mcpContext) {
        return "kb=" + kbContext.status() + "(" + kbContext.items().size() + ")"
                + " | mcp=" + mcpContext.status() + "(" + mcpContext.executions().size() + ")";
    }
}


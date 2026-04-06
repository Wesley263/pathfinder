package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import org.springframework.stereotype.Service;

/**
 * 检索阶段的默认编排器。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    /** 知识库分支检索器。 */
    private final KnowledgeRetriever knowledgeRetriever;
    /** 注释说明。 */
    private final McpContextExecutor mcpContextExecutor;

    /**
     * 说明。
     *
     * @param knowledgeRetriever 参数说明。
     * @param mcpContextExecutor 参数说明。
     */
    public DefaultRetrievalService(KnowledgeRetriever knowledgeRetriever, McpContextExecutor mcpContextExecutor) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.mcpContextExecutor = mcpContextExecutor;
    }

    /**
     * 说明。
     *
     * @param stageOneRagResult 第一阶段输出
     * @return 返回结果。
     */
    @Override
    public RetrievalResult retrieve(StageOneRagResult stageOneRagResult) {
        StageOneRagResult safeStageOneResult = stageOneRagResult == null
                ? new StageOneRagResult(null, null, null, null)
                : stageOneRagResult;

        // 说明。
        KbContext kbContext = knowledgeRetriever.retrieve(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());
        McpContext mcpContext = mcpContextExecutor.execute(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());

        // 说明。
        return new RetrievalResult(
                resolveStatus(kbContext, mcpContext),
                buildSummary(kbContext, mcpContext),
                safeStageOneResult,
                kbContext,
                mcpContext);
    }

    /**
     * 说明。
     *
     * @param kbContext 参数说明。
     * @param mcpContext 参数说明。
     * @return 返回结果。
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
     * 说明。
     *
     * @param kbContext 参数说明。
     * @param mcpContext 参数说明。
     * @return 简洁摘要文本
     */
    private String buildSummary(KbContext kbContext, McpContext mcpContext) {
        return "kb=" + kbContext.status() + "(" + kbContext.items().size() + ")"
                + " | mcp=" + mcpContext.status() + "(" + mcpContext.executions().size() + ")";
    }
}


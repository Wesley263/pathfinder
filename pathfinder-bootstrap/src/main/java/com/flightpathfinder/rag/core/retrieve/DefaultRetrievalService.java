package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import org.springframework.stereotype.Service;

/**
 * 检索阶段的默认编排器。
 *
 * <p>它位于 KB 与 MCP 两条分支之上，负责显式执行两路能力并统一汇总结果，
 * 这样 final answer 才能继续区分证据来源和部分成功语义。</p>
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    /** 知识库分支检索器。 */
    private final KnowledgeRetriever knowledgeRetriever;
    /** 工具分支执行器（MCP）。 */
    private final McpContextExecutor mcpContextExecutor;

    /**
     * 构造 retrieval 默认编排器。
     *
     * @param knowledgeRetriever KB 检索器
     * @param mcpContextExecutor MCP 执行器
     */
    public DefaultRetrievalService(KnowledgeRetriever knowledgeRetriever, McpContextExecutor mcpContextExecutor) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.mcpContextExecutor = mcpContextExecutor;
    }

    /**
     * 分别执行 KB 检索和 MCP 调用，再统一汇总成 retrieval 结果。
     *
     * @param stageOneRagResult 第一阶段输出
     * @return 同时保留 KB 与 MCP 子结果的 retrieval 总结果
     */
    @Override
    public RetrievalResult retrieve(StageOneRagResult stageOneRagResult) {
        StageOneRagResult safeStageOneResult = stageOneRagResult == null
                ? new StageOneRagResult(null, null, null, null)
                : stageOneRagResult;

        // 这里刻意保持 KB 与 MCP 分开，避免 final answer 只能接收一个已经混好的黑盒检索结果。
        KbContext kbContext = knowledgeRetriever.retrieve(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());
        McpContext mcpContext = mcpContextExecutor.execute(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());

        // 检索阶段是第一个同时看到 KB 成功、MCP miss 与混合部分成功语义的地方，因此总状态在这里收口。
        return new RetrievalResult(
                resolveStatus(kbContext, mcpContext),
                buildSummary(kbContext, mcpContext),
                safeStageOneResult,
                kbContext,
                mcpContext);
    }

    /**
     * 计算 retrieval 阶段总状态。
     *
     * @param kbContext KB 分支结果
     * @param mcpContext MCP 分支结果
     * @return retrieval 总状态
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
     * 生成 retrieval 阶段摘要。
     *
     * @param kbContext KB 分支结果
     * @param mcpContext MCP 分支结果
     * @return 简洁摘要文本
     */
    private String buildSummary(KbContext kbContext, McpContext mcpContext) {
        return "kb=" + kbContext.status() + "(" + kbContext.items().size() + ")"
                + " | mcp=" + mcpContext.status() + "(" + mcpContext.executions().size() + ")";
    }
}


package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import org.springframework.stereotype.Service;

/**
 * Default retrieval-stage orchestrator.
 *
 * <p>This class sits above KB and MCP branches because it keeps both branches explicit and
 * auditable before the final answer stage recombines them.</p>
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    private final KnowledgeRetriever knowledgeRetriever;
    private final McpContextExecutor mcpContextExecutor;

    public DefaultRetrievalService(KnowledgeRetriever knowledgeRetriever, McpContextExecutor mcpContextExecutor) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.mcpContextExecutor = mcpContextExecutor;
    }

    /**
     * Executes KB retrieval and MCP execution independently, then rejoins them into one
     * retrieval result for answer assembly.
     *
     * @param stageOneRagResult rewrite and split output from stage one
     * @return combined retrieval result with explicit KB and MCP sub-results
     */
    @Override
    public RetrievalResult retrieve(StageOneRagResult stageOneRagResult) {
        StageOneRagResult safeStageOneResult = stageOneRagResult == null
                ? new StageOneRagResult(null, null, null, null)
                : stageOneRagResult;

        // KB and MCP remain separate here so the answer stage can explain where evidence came
        // from instead of inheriting a pre-mixed opaque retrieval blob.
        KbContext kbContext = knowledgeRetriever.retrieve(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());
        McpContext mcpContext = mcpContextExecutor.execute(
                safeStageOneResult.rewriteResult(),
                safeStageOneResult.intentSplitResult());

        // Retrieval owns the combined stage status because it is the first place that can see
        // KB success, MCP misses, and mixed partial outcomes together.
        return new RetrievalResult(
                resolveStatus(kbContext, mcpContext),
                buildSummary(kbContext, mcpContext),
                safeStageOneResult,
                kbContext,
                mcpContext);
    }

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

    private String buildSummary(KbContext kbContext, McpContext mcpContext) {
        return "kb=" + kbContext.status() + "(" + kbContext.items().size() + ")"
                + " | mcp=" + mcpContext.status() + "(" + mcpContext.executions().size() + ")";
    }
}

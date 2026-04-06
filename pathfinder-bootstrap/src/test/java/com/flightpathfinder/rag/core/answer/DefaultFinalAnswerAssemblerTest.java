package com.flightpathfinder.rag.core.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.rag.core.intent.IntentKind;
import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.KbRetrievalItem;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class DefaultFinalAnswerAssemblerTest {

    private final DefaultFinalAnswerAssembler assembler = new DefaultFinalAnswerAssembler();

    @Test
    void assemble_shouldReturnEmptyPromptInputWhenRetrievalResultIsNull() {
        FinalAnswerPromptInput promptInput = assembler.assemble(null);

        assertTrue(promptInput.empty());
        assertTrue(promptInput.evidenceSummaries().isEmpty());
        assertTrue(promptInput.kbContext().empty());
        assertTrue(promptInput.mcpContext().executions().isEmpty());
    }

    @Test
    void assemble_shouldBuildEvidenceWhenKbAndMcpContextAreAvailable() {
        RetrievalResult retrievalResult = retrievalResult(
                "SUCCESS",
                kbContextWithItem(),
                mcpContextWithSuccessExecution(),
                splitResultWithKbAndMcp());

        FinalAnswerPromptInput promptInput = assembler.assemble(retrievalResult);

        assertEquals("改写问题", promptInput.rewrittenQuestion());
        assertFalse(promptInput.empty());
        assertFalse(promptInput.partial());
        assertFalse(promptInput.snapshotMissAffected());
        assertEquals(2, promptInput.evidenceSummaries().size());
        assertEquals("KB", promptInput.evidenceSummaries().get(0).type());
        assertEquals("MCP", promptInput.evidenceSummaries().get(1).type());
    }

    @Test
    void assemble_shouldMarkPartialWhenSnapshotMissAndOnlyKbProvidesMaterial() {
        RetrievalResult retrievalResult = retrievalResult(
                "SUCCESS",
                kbContextWithItem(),
                mcpContextWithSnapshotMissExecution(),
                splitResultWithKbAndMcp());

        FinalAnswerPromptInput promptInput = assembler.assemble(retrievalResult);

        assertTrue(promptInput.partial());
        assertTrue(promptInput.snapshotMissAffected());
        assertFalse(promptInput.empty());
    }

    private static RetrievalResult retrievalResult(
            String status,
            KbContext kbContext,
            McpContext mcpContext,
            IntentSplitResult intentSplitResult) {
        RewriteResult rewriteResult = new RewriteResult(
                "改写问题",
                List.of("改写问题"),
                "路由问题",
                List.of("路由问题"));
        IntentResolution intentResolution = new IntentResolution(List.of(), intentSplitResult);
        StageOneRagResult stageOneResult = new StageOneRagResult(
                rewriteResult,
                intentResolution,
                intentSplitResult,
                ConversationMemoryContext.empty("conv-1"));
        return new RetrievalResult(status, "summary", stageOneResult, kbContext, mcpContext);
    }

    private static IntentSplitResult splitResultWithKbAndMcp() {
        return new IntentSplitResult(List.of(kbIntent()), List.of(mcpIntent()), List.of());
    }

    private static ResolvedIntent kbIntent() {
        return new ResolvedIntent(
                "kb-1",
                "知识检索",
                IntentKind.KB,
                0.95,
                "子问题",
                "root/kb",
                "desc",
                "",
                "",
                "travel-kb",
                3);
    }

    private static ResolvedIntent mcpIntent() {
        return new ResolvedIntent(
                "mcp-1",
                "航班查询",
                IntentKind.MCP,
                0.92,
                "子问题",
                "root/mcp",
                "desc",
                "flight.search",
                "Flight Search",
                "",
                null);
    }

    private static KbContext kbContextWithItem() {
        KbRetrievalItem item = new KbRetrievalItem(
                "kb-1",
                "知识检索",
                "子问题",
                "travel-kb",
                "topic",
                "kb-doc",
                "doc-1",
                "航线提示",
                "建议先比较直飞与中转。",
                0.88,
                1);
        return new KbContext("SUCCESS", "kb ready", List.of(kbIntent()), List.of(item), false, "");
    }

    private static McpContext mcpContextWithSuccessExecution() {
        McpToolCallResult toolResult = new McpToolCallResult(
                "flight.search",
                true,
                "tool success",
                Map.of(
                        "flightCount", 1,
                        "flights", List.of(Map.of(
                                "airlineCode", "MU",
                                "origin", "PVG",
                                "destination", "NRT",
                                "date", "2026-05-01",
                                "priceCny", 1234,
                                "durationMinutes", 190))),
                "");
        McpExecutionRecord execution = new McpExecutionRecord(
                "mcp-1",
                "航班查询",
                "子问题",
                "flight.search",
                "LOCAL",
                Map.of("origin", "PVG", "destination", "NRT"),
                true,
                "SUCCESS",
                "ok",
                "",
                false,
                Boolean.FALSE,
                "",
                toolResult);
        return new McpContext("SUCCESS", "mcp ready", List.of(mcpIntent()), List.of(execution));
    }

    private static McpContext mcpContextWithSnapshotMissExecution() {
        McpExecutionRecord execution = new McpExecutionRecord(
                "mcp-1",
                "路径规划",
                "子问题",
                "graph.path.search",
                "LOCAL",
                Map.of("origin", "PEK", "destination", "NRT"),
                false,
                "SNAPSHOT_MISS",
                "snapshot not ready",
                "",
                true,
                Boolean.TRUE,
                "RETRY_LATER",
                null);
        return new McpContext("PARTIAL_FAILURE", "snapshot miss", List.of(mcpIntent()), List.of(execution));
    }
}


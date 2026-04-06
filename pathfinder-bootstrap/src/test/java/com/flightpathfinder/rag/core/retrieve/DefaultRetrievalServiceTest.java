package com.flightpathfinder.rag.core.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 测试用例。
 */

@ExtendWith(MockitoExtension.class)
class DefaultRetrievalServiceTest {

    @Mock
    private KnowledgeRetriever knowledgeRetriever;

    @Mock
    private McpContextExecutor mcpContextExecutor;

    private DefaultRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new DefaultRetrievalService(knowledgeRetriever, mcpContextExecutor);
    }

    @Test
    void retrieve_shouldReturnSuccessWhenKbAndMcpAreAvailable() {
        StageOneRagResult stageOneResult = stageOneResult();
        KbContext kbContext = kbSuccessContext();
        McpContext mcpContext = mcpContext("SUCCESS", List.of(execution("SUCCESS", false)));

        when(knowledgeRetriever.retrieve(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(kbContext);
        when(mcpContextExecutor.execute(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(mcpContext);

        RetrievalResult result = retrievalService.retrieve(stageOneResult);

        assertEquals("SUCCESS", result.status());
        assertEquals("kb=SUCCESS(1) | mcp=SUCCESS(1)", result.summary());
        assertEquals(1, result.kbContext().items().size());
        assertEquals(1, result.mcpContext().executions().size());
    }

    @Test
    void retrieve_shouldReturnSnapshotMissWhenMcpReportsSnapshotMiss() {
        StageOneRagResult stageOneResult = stageOneResult();

        when(knowledgeRetriever.retrieve(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(KbContext.empty("no kb", List.of()));
        when(mcpContextExecutor.execute(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(mcpContext("PARTIAL_FAILURE", List.of(execution("SNAPSHOT_MISS", true))));

        RetrievalResult result = retrievalService.retrieve(stageOneResult);

        assertEquals("SNAPSHOT_MISS", result.status());
    }

    @Test
    void retrieve_shouldReturnFailedWhenKbAndMcpBothFail() {
        StageOneRagResult stageOneResult = stageOneResult();

        when(knowledgeRetriever.retrieve(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(KbContext.error("kb error", List.of(), "timeout"));
        when(mcpContextExecutor.execute(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(mcpContext("FAILED", List.of(execution("ERROR", false))));

        RetrievalResult result = retrievalService.retrieve(stageOneResult);

        assertEquals("FAILED", result.status());
    }

    @Test
    void retrieve_shouldReturnPartialSuccessWhenMcpDataNotFoundButKbHasResult() {
        StageOneRagResult stageOneResult = stageOneResult();

        when(knowledgeRetriever.retrieve(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(kbSuccessContext());
        when(mcpContextExecutor.execute(stageOneResult.rewriteResult(), stageOneResult.intentSplitResult()))
                .thenReturn(mcpContext("DATA_NOT_FOUND", List.of(execution("DATA_NOT_FOUND", false))));

        RetrievalResult result = retrievalService.retrieve(stageOneResult);

        assertEquals("PARTIAL_SUCCESS", result.status());
    }

    @Test
    void retrieve_shouldUseDefaultStageOneWhenInputIsNull() {
        when(knowledgeRetriever.retrieve(any(RewriteResult.class), any(IntentSplitResult.class)))
                .thenReturn(KbContext.empty("no kb", List.of()));
        when(mcpContextExecutor.execute(any(RewriteResult.class), any(IntentSplitResult.class)))
                .thenReturn(McpContext.skipped("not executed"));

        RetrievalResult result = retrievalService.retrieve(null);

        assertEquals("EMPTY", result.status());
        assertNotNull(result.stageOneResult());
        assertEquals("", result.stageOneResult().rewriteResult().rewrittenQuestion());
        verify(knowledgeRetriever).retrieve(any(RewriteResult.class), any(IntentSplitResult.class));
        verify(mcpContextExecutor).execute(any(RewriteResult.class), any(IntentSplitResult.class));
    }

    private static StageOneRagResult stageOneResult() {
        RewriteResult rewriteResult = new RewriteResult(
                "改写问题",
                List.of("改写问题"),
                "路由问题",
                List.of("路由问题"));
        IntentSplitResult splitResult = IntentSplitResult.empty();
        return new StageOneRagResult(
                rewriteResult,
                new IntentResolution(List.of(), splitResult),
                splitResult,
                ConversationMemoryContext.empty("conv-1"));
    }

    private static KbContext kbSuccessContext() {
        KbRetrievalItem item = new KbRetrievalItem(
                "kb-1",
                "知识检索",
                "子问题",
                "travel-kb",
                "topic",
                "kb-doc",
                "doc-1",
                "航线提示",
                "建议优先比较中转与直飞。",
                0.92,
                1);
        return new KbContext("SUCCESS", "kb ready", List.of(), List.of(item), false, "");
    }

    private static McpContext mcpContext(String status, List<McpExecutionRecord> executions) {
        return new McpContext(status, "mcp summary", List.of(), executions);
    }

    private static McpExecutionRecord execution(String status, boolean snapshotMiss) {
        return new McpExecutionRecord(
                "mcp-1",
                "工具意图",
                "子问题",
                "flight.search",
                "LOCAL",
                Map.of("origin", "PVG", "destination", "NRT"),
                "SUCCESS".equals(status),
                status,
                "msg",
                "",
                snapshotMiss,
                Boolean.FALSE,
                "",
                null);
    }
}


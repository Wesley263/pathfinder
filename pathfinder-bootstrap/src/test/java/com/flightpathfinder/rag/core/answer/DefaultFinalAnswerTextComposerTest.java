package com.flightpathfinder.rag.core.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.KbRetrievalItem;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class DefaultFinalAnswerTextComposerTest {

    private final DefaultFinalAnswerTextComposer composer = new DefaultFinalAnswerTextComposer();

    @Test
    void compose_shouldReturnFallbackWhenPromptInputIsNull() {
        String text = composer.compose(null);

        assertEquals("No usable KB or MCP context is available for the current question yet.", text);
    }

    @Test
    void compose_shouldIncludeQuestionMcpAndKbSectionsWhenContextAvailable() {
        FinalAnswerPromptInput input = promptInput(kbContextWithItem(), mcpContextWithFlightSuccess(), false, false, false);

        String text = composer.compose(input);

        assertTrue(text.contains("Question: 请给出路线建议"));
        assertTrue(text.contains("MCP context:"));
        assertTrue(text.contains("Best flight:"));
        assertTrue(text.contains("KB context:"));
    }

    @Test
    void compose_shouldAppendSnapshotMessageWhenSnapshotMissAffected() {
        FinalAnswerPromptInput input = promptInput(kbContextWithItem(), McpContext.skipped("not ready"), true, true, false);

        String text = composer.compose(input);

        assertTrue(text.contains("Graph snapshot status:"));
    }

    private static FinalAnswerPromptInput promptInput(
            KbContext kbContext,
            McpContext mcpContext,
            boolean partial,
            boolean snapshotMissAffected,
            boolean empty) {
        return new FinalAnswerPromptInput(
                "请给出路线建议",
                IntentSplitResult.empty(),
                kbContext,
                mcpContext,
                ConversationMemoryContext.empty("conv-1"),
                List.of(),
                partial,
                snapshotMissAffected,
                empty);
    }

    private static KbContext kbContextWithItem() {
        KbRetrievalItem item = new KbRetrievalItem(
                "kb-intent-1",
                "知识问答",
                "路线建议",
                "travel-kb",
                "topic",
                "kb-doc",
                "doc-1",
                "航线提示",
                "推荐先比较直飞和中转。",
                0.91,
                1);
        return new KbContext("SUCCESS", "kb ready", List.of(), List.of(item), false, "");
    }

    private static McpContext mcpContextWithFlightSuccess() {
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
                                "priceCny", 1200,
                                "durationMinutes", 180))),
                "");
        McpExecutionRecord execution = new McpExecutionRecord(
                "mcp-intent-1",
                "航班查询",
                "查航班",
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
        return new McpContext("SUCCESS", "mcp ready", List.of(), List.of(execution));
    }
}




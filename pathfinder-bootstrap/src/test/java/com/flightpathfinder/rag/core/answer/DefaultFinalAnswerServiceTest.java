package com.flightpathfinder.rag.core.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.KbRetrievalItem;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class DefaultFinalAnswerServiceTest {

    @Test
    void answer_shouldReturnEmptyResultAndSkipComposerWhenPromptInputIsEmpty() {
        AtomicBoolean composerCalled = new AtomicBoolean(false);
        FinalAnswerAssembler assembler = retrievalResult -> promptInput(
                KbContext.empty("no kb", List.of()),
                McpContext.skipped("no mcp"),
                false,
                false,
                true);
        FinalAnswerTextComposer composer = promptInput -> {
            composerCalled.set(true);
            return "should-not-be-used";
        };

        DefaultFinalAnswerService service = new DefaultFinalAnswerService(assembler, composer);
        AnswerResult result = service.answer(anyRetrievalResult());

        assertEquals("EMPTY", result.status());
        assertTrue(result.answerText().contains("No final answer can be assembled yet"));
        assertFalse(composerCalled.get());
    }

    @Test
    void answer_shouldReturnSuccessWhenPromptInputIsComplete() {
        FinalAnswerAssembler assembler = retrievalResult -> promptInput(
                kbContextWithItem(),
                McpContext.skipped("no mcp needed"),
                false,
                false,
                false);
        FinalAnswerTextComposer composer = promptInput -> "最终回答";

        DefaultFinalAnswerService service = new DefaultFinalAnswerService(assembler, composer);
        AnswerResult result = service.answer(anyRetrievalResult());

        assertEquals("SUCCESS", result.status());
        assertEquals("最终回答", result.answerText());
        assertFalse(result.partial());
        assertFalse(result.snapshotMissAffected());
    }

    @Test
    void answer_shouldResolvePartialWhenSnapshotMissAndPartialFlagsAreTrue() {
        FinalAnswerAssembler assembler = retrievalResult -> promptInput(
                kbContextWithItem(),
                McpContext.skipped("snapshot miss elsewhere"),
                true,
                true,
                false);
        FinalAnswerTextComposer composer = promptInput -> "部分回答";

        DefaultFinalAnswerService service = new DefaultFinalAnswerService(assembler, composer);
        AnswerResult result = service.answer(anyRetrievalResult());

        assertEquals("PARTIAL", result.status());
        assertTrue(result.partial());
        assertTrue(result.snapshotMissAffected());
    }

    @Test
    void answer_shouldResolveDataNotFoundWhenMcpReturnsDataNotFoundAndKbIsEmpty() {
        FinalAnswerAssembler assembler = retrievalResult -> promptInput(
                KbContext.empty("no kb", List.of()),
                new McpContext("DATA_NOT_FOUND", "not found", List.of(), List.of()),
                false,
                false,
                false);
        FinalAnswerTextComposer composer = promptInput -> "暂无数据";

        DefaultFinalAnswerService service = new DefaultFinalAnswerService(assembler, composer);
        AnswerResult result = service.answer(anyRetrievalResult());

        assertEquals("DATA_NOT_FOUND", result.status());
        assertEquals("暂无数据", result.answerText());
    }

    private static RetrievalResult anyRetrievalResult() {
        return new RetrievalResult("SUCCESS", "summary", null, null, null);
    }

    private static FinalAnswerPromptInput promptInput(
            KbContext kbContext,
            McpContext mcpContext,
            boolean partial,
            boolean snapshotMissAffected,
            boolean empty) {
        return new FinalAnswerPromptInput(
                "改写问题",
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
                "kb-1",
                "知识检索",
                "子问题",
                "travel-kb",
                "topic",
                "kb-doc",
                "doc-1",
                "航线提示",
                "建议优先比较中转时长与价格。",
                0.9,
                1);
        return new KbContext("SUCCESS", "kb ready", List.of(), List.of(item), false, "");
    }
}




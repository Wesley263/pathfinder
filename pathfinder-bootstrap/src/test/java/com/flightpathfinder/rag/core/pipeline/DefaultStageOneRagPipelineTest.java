package com.flightpathfinder.rag.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentResolver;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.rewrite.QuestionRewriteService;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultStageOneRagPipelineTest {

    @Mock
    private QuestionRewriteService questionRewriteService;

    @Mock
    private IntentResolver intentResolver;

    private DefaultStageOneRagPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new DefaultStageOneRagPipeline(questionRewriteService, intentResolver);
    }

    @Test
    void run_shouldRewriteThenResolveAndCarryMemoryContext() {
        ConversationMemoryContext memoryContext = ConversationMemoryContext.empty("conv-1");
        StageOneRagRequest request = new StageOneRagRequest("  用户问题  ", "conv-1", "req-1", memoryContext);
        RewriteResult rewriteResult = new RewriteResult(
                "改写问题",
                List.of("改写问题"),
                "路由问题",
                List.of("路由问题"));
        IntentSplitResult splitResult = IntentSplitResult.empty();
        IntentResolution intentResolution = new IntentResolution(List.of(), splitResult);

        when(questionRewriteService.rewrite(request)).thenReturn(rewriteResult);
        when(intentResolver.resolve(rewriteResult)).thenReturn(intentResolution);

        StageOneRagResult result = pipeline.run(request);

        assertSame(rewriteResult, result.rewriteResult());
        assertSame(intentResolution, result.intentResolution());
        assertSame(splitResult, result.intentSplitResult());
        assertSame(memoryContext, result.memoryContext());

        InOrder inOrder = inOrder(questionRewriteService, intentResolver);
        inOrder.verify(questionRewriteService).rewrite(request);
        inOrder.verify(intentResolver).resolve(rewriteResult);
    }

    @Test
    void run_shouldReturnEmptyMemoryContextWhenRequestIsNull() {
        RewriteResult rewriteResult = new RewriteResult(
                "改写问题",
                List.of("改写问题"),
                "路由问题",
                List.of("路由问题"));
        IntentResolution intentResolution = IntentResolution.empty();

        when(questionRewriteService.rewrite(null)).thenReturn(rewriteResult);
        when(intentResolver.resolve(rewriteResult)).thenReturn(intentResolution);

        StageOneRagResult result = pipeline.run(null);

        assertEquals("", result.memoryContext().conversationId());
        assertTrue(result.memoryContext().empty());
    }
}




package com.flightpathfinder.rag.core.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class DefaultQuestionRewriteServiceTest {

    private final DefaultQuestionRewriteService service = new DefaultQuestionRewriteService();

    @Test
    void rewrite_shouldNormalizeTermsAndSplitSubQuestions() {
        StageOneRagRequest request = new StageOneRagRequest(
                "帮我查上海到东京的航班；另外看看签证要求",
                "conv-1",
                "req-1",
                ConversationMemoryContext.empty("conv-1"));

        RewriteResult result = service.rewrite(request);

        assertEquals("帮我查SHA到TYO的航班；另外看看签证要求", result.rewrittenQuestion());
        assertEquals(List.of("帮我查SHA到TYO的航班", "看看签证要求"), result.subQuestions());
        assertEquals(result.rewrittenQuestion(), result.routingQuestion());
        assertEquals(result.subQuestions(), result.routingSubQuestions());
    }

    @Test
    void rewrite_shouldInjectMemoryIntoRoutingQuestionForFollowUp() {
        ConversationMemoryContext memoryContext = new ConversationMemoryContext(
                null,
                new ConversationMemorySummary("上一轮在讨论 SHA 到 TYO 的行程", 1, Instant.now()),
                List.of());
        StageOneRagRequest request = new StageOneRagRequest("那价格呢", "conv-2", "req-2", memoryContext);

        RewriteResult result = service.rewrite(request);

        assertEquals("那价格呢", result.rewrittenQuestion());
        assertEquals(List.of("那价格呢"), result.subQuestions());
        assertTrue(result.routingQuestion().startsWith("Conversation context: "));
        assertTrue(result.routingQuestion().contains("Summary: 上一轮在讨论 SHA 到 TYO 的行程"));
        assertTrue(result.routingQuestion().endsWith("Current question: 那价格呢"));
        assertEquals(1, result.routingSubQuestions().size());
        assertTrue(result.routingSubQuestions().get(0).endsWith("Current question: 那价格呢"));
    }
}




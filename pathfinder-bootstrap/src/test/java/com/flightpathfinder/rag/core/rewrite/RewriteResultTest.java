package com.flightpathfinder.rag.core.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class RewriteResultTest {

    @Test
    void constructor_shouldSanitizeFieldsAndFallbackRoutingValues() {
        RewriteResult result = new RewriteResult(
                "  改写问题  ",
                Arrays.asList("  子问题1  ", " ", null),
                "   ",
                Arrays.asList(" ", null));

        assertEquals("改写问题", result.rewrittenQuestion());
        assertEquals(List.of("子问题1"), result.subQuestions());
        assertEquals("改写问题", result.routingQuestion());
        assertEquals(List.of("改写问题"), result.routingSubQuestions());
    }

    @Test
    void constructor_shouldProvideStableEmptyFallbackWhenAllInputsMissing() {
        RewriteResult result = new RewriteResult(" ", null, null, null);

        assertEquals("", result.rewrittenQuestion());
        assertEquals(List.of(""), result.subQuestions());
        assertEquals("", result.routingQuestion());
        assertEquals(List.of(""), result.routingSubQuestions());
    }
}




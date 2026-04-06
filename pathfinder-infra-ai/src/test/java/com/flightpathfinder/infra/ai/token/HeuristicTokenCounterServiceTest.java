package com.flightpathfinder.infra.ai.token;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class HeuristicTokenCounterServiceTest {

    private final HeuristicTokenCounterService service = new HeuristicTokenCounterService();

    @Test
    void estimateTokens_shouldReturnZeroForNullOrBlankInput() {
        assertEquals(0, service.estimateTokens(null));
        assertEquals(0, service.estimateTokens("   "));
    }

    @Test
    void estimateTokens_shouldEstimateByQuarterLengthWithCeil() {
        assertEquals(1, service.estimateTokens("a"));
        assertEquals(1, service.estimateTokens("abcd"));
        assertEquals(2, service.estimateTokens("abcde"));
        assertEquals(3, service.estimateTokens("abcdefghij"));
    }
}


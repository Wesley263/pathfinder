package com.flightpathfinder.rag.core.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class AnswerResultTest {

    @Test
    void constructor_shouldNormalizeStatusAndTextAndEmptyFlag() {
        AnswerResult result = new AnswerResult(
                " ",
                "   ",
                false,
                false,
                false,
                List.of(new AnswerEvidenceSummary("MCP", "t", "label", "SUCCESS", "snippet")));

        assertEquals("EMPTY", result.status());
        assertEquals("", result.answerText());
        assertTrue(result.empty());
        assertEquals(1, result.evidenceSummaries().size());
    }

    @Test
    void emptyFactory_shouldCreateEmptyStatusResult() {
        AnswerResult result = AnswerResult.empty("no data", List.of());

        assertEquals("EMPTY", result.status());
        assertEquals("no data", result.answerText());
        assertTrue(result.empty());
    }
}




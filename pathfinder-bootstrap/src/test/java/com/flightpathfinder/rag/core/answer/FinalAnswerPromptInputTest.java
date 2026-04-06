package com.flightpathfinder.rag.core.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class FinalAnswerPromptInputTest {

    @Test
    void constructor_shouldNormalizeNullFieldsAndCopyEvidence() {
        List<AnswerEvidenceSummary> evidences = new ArrayList<>();
        evidences.add(new AnswerEvidenceSummary("KB", "s", "l", "SUCCESS", "snippet"));

        FinalAnswerPromptInput input = new FinalAnswerPromptInput(
                null,
                null,
                null,
                null,
                null,
                evidences,
                true,
                true,
                false);
        evidences.clear();

        assertEquals("", input.rewrittenQuestion());
        assertTrue(input.intentSplitResult().kbIntents().isEmpty());
        assertTrue(input.kbContext().empty());
        assertTrue(input.mcpContext().executions().isEmpty());
        assertEquals(1, input.evidenceSummaries().size());
        assertTrue(input.partial());
        assertTrue(input.snapshotMissAffected());
    }
}




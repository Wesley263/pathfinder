package com.flightpathfinder.rag.controller.vo;

import java.util.List;

public record RagChatResponseVO(
        String question,
        String rewrittenQuestion,
        List<String> subQuestions,
        String answerText,
        boolean partial,
        boolean snapshotMissAffected,
        RagAuditVO audit,
        List<String> kbIntentIds,
        List<String> mcpIntentIds,
        List<String> systemIntentIds,
        List<RagAnswerEvidenceVO> evidenceSummaries) {
}

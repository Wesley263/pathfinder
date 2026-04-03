package com.flightpathfinder.rag.core.answer;

import java.util.List;

public record AnswerResult(
        String status,
        String answerText,
        boolean partial,
        boolean snapshotMissAffected,
        boolean empty,
        List<AnswerEvidenceSummary> evidenceSummaries) {

    public AnswerResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        answerText = answerText == null ? "" : answerText.trim();
        evidenceSummaries = List.copyOf(evidenceSummaries == null ? List.of() : evidenceSummaries);
        empty = empty || answerText.isBlank();
    }

    public static AnswerResult empty(String answerText, List<AnswerEvidenceSummary> evidenceSummaries) {
        return new AnswerResult("EMPTY", answerText, false, false, true, evidenceSummaries);
    }
}

package com.flightpathfinder.rag.core.answer;

public record AnswerEvidenceSummary(
        String type,
        String source,
        String label,
        String status,
        String snippet) {

    public AnswerEvidenceSummary {
        type = type == null ? "" : type.trim();
        source = source == null ? "" : source.trim();
        label = label == null ? "" : label.trim();
        status = status == null ? "" : status.trim();
        snippet = snippet == null ? "" : snippet.trim();
    }
}

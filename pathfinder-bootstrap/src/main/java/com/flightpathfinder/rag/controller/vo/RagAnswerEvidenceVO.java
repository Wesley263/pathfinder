package com.flightpathfinder.rag.controller.vo;

public record RagAnswerEvidenceVO(
        String type,
        String source,
        String label,
        String status,
        String snippet) {
}

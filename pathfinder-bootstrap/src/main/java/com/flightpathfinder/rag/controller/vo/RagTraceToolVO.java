package com.flightpathfinder.rag.controller.vo;

public record RagTraceToolVO(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {
}

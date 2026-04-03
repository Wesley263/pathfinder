package com.flightpathfinder.rag.service.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record RagStreamEvent(
        long sequence,
        String event,
        String stage,
        String requestId,
        String conversationId,
        String traceId,
        String timestamp,
        String status,
        Boolean partial,
        Boolean snapshotMissAffected,
        String error,
        Map<String, Object> data) {

    public RagStreamEvent {
        event = event == null ? "" : event.trim();
        stage = stage == null ? "" : stage.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        traceId = traceId == null ? "" : traceId.trim();
        timestamp = timestamp == null ? "" : timestamp.trim();
        status = status == null ? "" : status.trim();
        error = error == null ? "" : error.trim();
        data = Map.copyOf(new LinkedHashMap<>(data == null ? Map.of() : data));
    }
}

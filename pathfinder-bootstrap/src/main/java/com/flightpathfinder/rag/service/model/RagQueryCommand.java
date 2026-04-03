package com.flightpathfinder.rag.service.model;

public record RagQueryCommand(String question, String conversationId, String requestId) {

    public RagQueryCommand {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
    }
}

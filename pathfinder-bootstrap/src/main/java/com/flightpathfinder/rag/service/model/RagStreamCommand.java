package com.flightpathfinder.rag.service.model;

public record RagStreamCommand(String question, String conversationId, String requestId) {

    public RagStreamCommand {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
    }
}

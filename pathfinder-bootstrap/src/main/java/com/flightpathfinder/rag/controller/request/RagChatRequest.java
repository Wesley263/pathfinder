package com.flightpathfinder.rag.controller.request;

import jakarta.validation.constraints.NotBlank;

public record RagChatRequest(
        @NotBlank(message = "question cannot be blank")
        String question,
        String conversationId) {

    public RagChatRequest {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
    }
}

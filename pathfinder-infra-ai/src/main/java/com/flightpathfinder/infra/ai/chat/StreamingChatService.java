package com.flightpathfinder.infra.ai.chat;

public interface StreamingChatService {

    void stream(ChatRequest request, StreamCallback callback);
}


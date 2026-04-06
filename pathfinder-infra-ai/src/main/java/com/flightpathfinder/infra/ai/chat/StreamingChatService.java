package com.flightpathfinder.infra.ai.chat;
/**
 * 说明。
 */
public interface StreamingChatService {

    void stream(ChatRequest request, StreamCallback callback);
}




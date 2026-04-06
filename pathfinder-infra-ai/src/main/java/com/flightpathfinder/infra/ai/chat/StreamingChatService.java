package com.flightpathfinder.infra.ai.chat;
/**
 * 面向 AI 聊天的能力接口定义。
 */
public interface StreamingChatService {

    void stream(ChatRequest request, StreamCallback callback);
}




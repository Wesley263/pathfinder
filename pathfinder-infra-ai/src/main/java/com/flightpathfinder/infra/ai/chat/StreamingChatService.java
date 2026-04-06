package com.flightpathfinder.infra.ai.chat;
/**
 * 流式聊天服务抽象。
 */
public interface StreamingChatService {

    /**
     * 执行一次流式聊天调用。
     *
     * @param request 聊天请求
     * @param callback 流式回调
     */
    void stream(ChatRequest request, StreamCallback callback);
}




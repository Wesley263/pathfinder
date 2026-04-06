package com.flightpathfinder.infra.ai.chat;
/**
 * 同步聊天服务抽象。
 */
public interface ChatService {

    /**
     * 执行一次同步聊天调用。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
}




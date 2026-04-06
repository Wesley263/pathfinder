package com.flightpathfinder.infra.ai.chat;
/**
 * 面向 AI 聊天的能力接口定义。
 */
public interface ChatService {

    ChatResponse chat(ChatRequest request);
}




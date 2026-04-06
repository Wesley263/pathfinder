package com.flightpathfinder.infra.ai.chat;
/**
 * 面向 AI 聊天的请求与响应模型。
 */
public record ChatResponse(String content, String modelName, boolean placeholder) {
}




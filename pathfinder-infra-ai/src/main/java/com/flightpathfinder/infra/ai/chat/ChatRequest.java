package com.flightpathfinder.infra.ai.chat;

import java.util.Map;
/**
 * 面向 AI 聊天的请求与响应模型。
 */
public record ChatRequest(String userPrompt, String systemPrompt, Map<String, Object> metadata) {
}




package com.flightpathfinder.infra.ai.chat;

import java.util.Map;
/**
 * 聊天请求载荷。
 *
 * @param userPrompt 用户输入的主问题
 * @param systemPrompt 系统提示词
 * @param metadata 调用侧附带的扩展元数据
 */
public record ChatRequest(String userPrompt, String systemPrompt, Map<String, Object> metadata) {
}




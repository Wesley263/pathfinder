package com.flightpathfinder.infra.ai.chat;
/**
 * 聊天响应载荷。
 *
 * @param content 模型返回文本
 * @param modelName 本次使用的模型名
 * @param placeholder 是否为占位响应
 */
public record ChatResponse(String content, String modelName, boolean placeholder) {
}




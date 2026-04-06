package com.flightpathfinder.rag.controller.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户面对话接口请求体。
 *
 * <p>它同时服务于同步和 SSE 入口，conversationId 为空时按无记忆对话处理。</p>
 *
 * @param question 用户输入的问题文本
 * @param conversationId 可选会话标识
 */
public record RagChatRequest(
        @NotBlank(message = "question cannot be blank")
        String question,
        String conversationId) {

    /**
     * 归一化请求体字段。
     */
    public RagChatRequest {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
    }
}

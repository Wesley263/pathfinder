package com.flightpathfinder.rag.service.model;

/**
 * 流式查询命令。
 *
 * 说明。
 *
 * @param question 用户问题
 * @param conversationId 会话标识
 * @param requestId 请求标识
 */
public record RagStreamCommand(String question, String conversationId, String requestId) {

    /**
     * 归一化流式查询命令。
     */
    public RagStreamCommand {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
    }
}

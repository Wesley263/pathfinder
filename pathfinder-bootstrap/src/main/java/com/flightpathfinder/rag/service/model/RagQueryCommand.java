package com.flightpathfinder.rag.service.model;

/**
 * 同步查询命令。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param question 用户问题
 * @param conversationId 会话标识
 * @param requestId 请求标识
 */
public record RagQueryCommand(String question, String conversationId, String requestId) {

    /**
     * 归一化同步查询命令。
     */
    public RagQueryCommand {
        question = question == null ? "" : question.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
    }
}



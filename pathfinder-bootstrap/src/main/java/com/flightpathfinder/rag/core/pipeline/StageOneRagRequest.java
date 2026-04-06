package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;

/**
 * 第一阶段主链的输入模型。
 *
 * 汇总改写与意图解析阶段所需的请求上下文。
 *
 * @param originalQuestion 用户本轮提交的原始问题
 * @param conversationId 当前会话标识；为空时表示按无记忆对话处理
 * @param requestId 请求标识
 * @param memoryContext 查询前已加载的只读会话记忆上下文
 */
public record StageOneRagRequest(
        String originalQuestion,
        String conversationId,
        String requestId,
        ConversationMemoryContext memoryContext) {

    /**
     * 归一化第一阶段请求对象。
     *
        * 确保关键文本字段非空，并在缺失记忆时补充空上下文。
     */
    public StageOneRagRequest {
        originalQuestion = originalQuestion == null ? "" : originalQuestion.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        memoryContext = memoryContext == null
                ? ConversationMemoryContext.empty(conversationId)
                : memoryContext;
    }
}



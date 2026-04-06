package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;

/**
 * 第一阶段主链的输入模型。
 *
 * <p>它把原始问题、请求标识和 memory 上下文打包成一个稳定的入参对象，避免 rewrite 层直接感知
 * 屏蔽 Web 请求细节，也避免后续新增请求级上下文时不断改方法签名。</p>
 *
 * @param originalQuestion 用户本轮提交的原始问题
 * @param conversationId 当前会话标识；为空时表示按无记忆对话处理
 * @param requestId 当前请求标识，用于串联日志、trace 和响应审计信息
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
     * <p>这里统一处理空值和空白值，保证下游 rewrite / intent 逻辑看到的是可直接消费的稳定数据结构。</p>
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


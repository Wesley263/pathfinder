package com.flightpathfinder.rag.core.memory;

/**
 * 会话记忆能力对应用层暴露的统一入口。
 *
 * <p>该服务位于存储组件与摘要组件之上，使 RAG 编排层在加载和写入会话上下文时，
 * 无需感知底层是 JDBC、内存回退实现，还是摘要压缩策略。
 */
public interface ConversationMemoryService {

    /**
     * 加载指定会话的记忆上下文。
     *
     * @param conversationId 用户侧 API 透传的稳定会话标识；为空表示“不启用记忆”
     * @return 包含近期轮次与可选摘要的记忆上下文；当标识为空或无历史时返回空上下文
     */
    ConversationMemoryContext loadContext(String conversationId);

    /**
     * 将当前用户/助手轮次追加写入记忆存储。
     *
     * @param writeRequest 已归一化的完成轮次写入请求
     */
    void appendTurn(ConversationMemoryWriteRequest writeRequest);
}

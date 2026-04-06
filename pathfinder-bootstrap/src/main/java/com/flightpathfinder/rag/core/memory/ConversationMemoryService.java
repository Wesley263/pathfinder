package com.flightpathfinder.rag.core.memory;

/**
 * 会话记忆能力对应用层暴露的统一入口。
 *
 * 对外提供记忆加载与轮次写入能力，屏蔽底层存储细节。
 */
public interface ConversationMemoryService {

    /**
     * 加载指定会话的记忆上下文。
     *
        * @param conversationId 会话标识
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


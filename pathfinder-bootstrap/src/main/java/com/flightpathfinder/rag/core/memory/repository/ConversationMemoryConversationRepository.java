package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryConversation;
import java.util.List;
import java.util.Optional;

/**
 * 会话级记忆元数据仓储接口。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 * 消息明细与摘要分属独立仓储，以便各层能力独立演进。
 */
public interface ConversationMemoryConversationRepository {

    /**
     * 按会话标识查询单条会话头信息。
     *
     * @param conversationId 稳定会话标识
     * @return 命中时返回会话元数据
     */
    Optional<ConversationMemoryConversation> findByConversationId(String conversationId);

    /**
     * 查询近期会话列表，供管理与检索场景使用。
     *
     * @param conversationId 可选精确过滤；为空表示“列出近期会话”
     * @param limit 最大返回条数
     * @return 按更新时间排序的近期会话头列表
     */
    List<ConversationMemoryConversation> findRecent(String conversationId, int limit);

    /**
     * 在写入完成轮次后更新或插入会话头。
     *
     * @param conversationId 稳定会话标识
     * @param requestId 与最新完成轮次关联的请求标识
     */
    void upsertAfterTurn(String conversationId, String requestId);
}



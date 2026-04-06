package com.flightpathfinder.rag.core.memory;

/**
 * 会话记忆摘要管理抽象。
 *
 * 说明。
 * 主链因此可以按需使用近期轮次、摘要或二者组合，而不把写存储与摘要策略耦合在一起。
 */
public interface ConversationMemorySummaryService {

    /**
     * 在启用摘要能力时加载会话最新摘要。
     *
     * @param conversationId 稳定会话标识
     * @return 已持久化摘要；不可用时返回空摘要对象
     */
    ConversationMemorySummary loadLatestSummary(String conversationId);

    /**
     * 当会话规模超过阈值时刷新摘要状态。
     *
     * @param conversationId 稳定会话标识
     */
    void refreshIfNeeded(String conversationId);
}

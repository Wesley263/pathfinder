package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import java.util.Optional;

/**
 * 会话摘要持久化仓储接口。
 *
 * <p>摘要存储与原始消息分离，
 * 以便在不修改底层会话历史的前提下重算或替换摘要文本。
 */
public interface ConversationMemorySummaryRepository {

    /**
     * 查询指定会话的最新摘要。
     *
     * @param conversationId 稳定会话标识
     * @return 命中时返回已持久化摘要
     */
    Optional<ConversationMemorySummary> findByConversationId(String conversationId);

    /**
     * 为指定会话插入或更新摘要文本。
     *
     * @param conversationId 稳定会话标识
     * @param summaryText 压缩后的摘要文本
     * @param summarizedTurnCount 已由摘要覆盖的历史轮次数
     */
    void upsert(String conversationId, String summaryText, int summarizedTurnCount);
}

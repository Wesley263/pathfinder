package com.flightpathfinder.rag.core.memory;

import java.time.Instant;

/**
 * 会话记忆摘要实体。
 *
 * @param summaryText 摘要文本
 * @param summarizedTurnCount 已被摘要覆盖的历史轮次数
 * @param updatedAt 摘要更新时间
 */
public record ConversationMemorySummary(
        String summaryText,
        int summarizedTurnCount,
        Instant updatedAt) {

    /**
     * 构造时完成文本规整与轮次数下界保护。
     */
    public ConversationMemorySummary {
        summaryText = summaryText == null ? "" : summaryText.trim();
        summarizedTurnCount = Math.max(0, summarizedTurnCount);
    }

    /**
     * 判断摘要文本是否存在。
     *
     * @return 返回结果。
     */
    public boolean exists() {
        return !summaryText.isBlank();
    }

    /**
     * 创建空摘要对象。
     *
     * @return 空摘要
     */
    public static ConversationMemorySummary empty() {
        return new ConversationMemorySummary("", 0, null);
    }
}

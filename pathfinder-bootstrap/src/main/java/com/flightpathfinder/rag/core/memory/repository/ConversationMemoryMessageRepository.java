package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryTurn;
import java.util.List;

/**
 * 原始会话消息行仓储接口。
 *
 * <p>消息按行存储而非直接组装成轮次，
 * 便于用户与助手内容独立审计，并可在后续重组为不同上层投影视图。
 */
public interface ConversationMemoryMessageRepository {

    /**
     * 将一轮完成对话持久化为显式 USER 与 ASSISTANT 两条消息行。
     *
     * @param conversationId 稳定会话标识
     * @param turn 待持久化的完成轮次
     */
    void saveTurn(String conversationId, ConversationMemoryTurn turn);

    /**
     * 加载会话近期消息行。
     *
     * @param conversationId 稳定会话标识
     * @param limit 最大返回消息行数
     * @return 为轮次重组准备好的近期消息行
     */
    List<ConversationMemoryMessageRecord> findRecentByConversationId(String conversationId, int limit);

    /**
     * 加载会话全部消息行。
     *
     * @param conversationId 稳定会话标识
     * @return 按时间从旧到新排序的完整消息行
     */
    List<ConversationMemoryMessageRecord> findAllByConversationId(String conversationId);
}

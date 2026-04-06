package com.flightpathfinder.rag.core.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

/**
 * 会话记忆的最小内存回退实现。
 *
 * <p>该 Bean 仅在未配置持久化存储时生效，
 * 用于保证本地开发场景在不改变上层记忆契约的前提下仍可运行。
 */
@Repository
@ConditionalOnMissingBean(ConversationMemoryStore.class)
public class InMemoryConversationMemoryStore implements ConversationMemoryStore {

    private static final int MAX_STORED_TURNS = 20;

    private final Map<String, Deque<ConversationMemoryTurn>> conversations = new ConcurrentHashMap<>();

    /**
     * 从内存回退存储中加载近期轮次。
     *
     * @param conversationId 稳定会话标识
     * @param recentTurnLimit 请求的近期轮次数量
     * @return 近期轮次快照；无存储轮次时返回空快照
     */
    @Override
    public ConversationMemorySnapshot load(String conversationId, int recentTurnLimit) {
        if (conversationId == null || conversationId.isBlank()) {
            return ConversationMemorySnapshot.empty("");
        }
        Deque<ConversationMemoryTurn> turns = conversations.get(conversationId);
        if (turns == null || turns.isEmpty()) {
            return ConversationMemorySnapshot.empty(conversationId);
        }
        List<ConversationMemoryTurn> storedTurns = new ArrayList<>(turns);
        int fromIndex = Math.max(0, storedTurns.size() - Math.max(1, recentTurnLimit));
        return new ConversationMemorySnapshot(
                new ConversationMemoryConversation(conversationId, "", storedTurns.size(), storedTurns.size() * 2, null, null),
                List.copyOf(storedTurns.subList(fromIndex, storedTurns.size())));
    }

    /**
     * 向内存回退存储追加一轮完成对话。
     *
     * @param conversationId 稳定会话标识
     * @param turn 待存储的完成轮次
     */
    @Override
    public void appendTurn(String conversationId, ConversationMemoryTurn turn) {
        if (conversationId == null || conversationId.isBlank() || turn == null) {
            return;
        }
        conversations.compute(conversationId, (key, existingTurns) -> {
            Deque<ConversationMemoryTurn> turns = existingTurns == null ? new ArrayDeque<>() : new ArrayDeque<>(existingTurns);
            turns.addLast(turn);
            // 回退存储有意设置上界，避免本地开发会话在堆内无限增长。
            while (turns.size() > MAX_STORED_TURNS) {
                turns.removeFirst();
            }
            return turns;
        });
    }
}

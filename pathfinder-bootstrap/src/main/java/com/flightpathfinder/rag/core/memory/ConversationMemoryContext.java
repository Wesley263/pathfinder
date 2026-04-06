package com.flightpathfinder.rag.core.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话记忆上下文。
 *
 * 聚合会话元信息、摘要与近期轮次，供路由改写阶段拼装历史上下文。
 */
public record ConversationMemoryContext(
        ConversationMemoryConversation conversation,
        ConversationMemorySummary summary,
        List<ConversationMemoryTurn> recentTurns) {

    public ConversationMemoryContext {
        conversation = conversation == null
                ? ConversationMemoryConversation.empty("")
                : conversation;
        summary = summary == null ? ConversationMemorySummary.empty() : summary;
        recentTurns = List.copyOf(recentTurns == null ? List.of() : recentTurns);
    }

    /**
     * 判断当前上下文是否不含可用记忆。
     *
     * @return 无摘要且无近期轮次时返回 true
     */
    public boolean empty() {
        return recentTurns.isEmpty() && !summary.exists();
    }

    /**
     * 返回与当前记忆上下文关联的会话标识。
     *
     * @return 会话标识；当请求不使用记忆时可能为空
     */
    public String conversationId() {
        return conversation.conversationId();
    }

    /**
     * 返回该会话已持久化的总轮次数。
     *
     * @return 会话元信息中的总轮次
     */
    public int totalTurnCount() {
        return conversation.turnCount();
    }

    /**
     * 判断是否存在可用摘要片段。
     *
     * @return 存在摘要时返回 true
     */
    public boolean hasSummary() {
        return summary.exists();
    }

    /**
     * 将摘要与近期轮次压平成主链使用的紧凑历史文本。
     *
     * @return 便于路由阶段消费的历史片段；无记忆可注入时返回空字符串
     */
    public String routingHistoryText() {
        List<String> fragments = new ArrayList<>();
        if (summary.exists()) {
            fragments.add("Summary: " + summary.summaryText());
        }
        // 摘要覆盖较早历史，近期轮次保留最新精确措辞。
        // 以稳定模板拼接历史文本，便于下游提示词与日志对齐。
        recentTurns.stream()
                .map(turn -> "User: " + turn.questionForContext() + " | Assistant: " + abbreviate(turn.answerText()))
                .forEach(fragments::add);
        if (fragments.isEmpty()) {
            return "";
        }
        return fragments.stream()
                .reduce((left, right) -> left + " || " + right)
                .orElse("");
    }

    /**
     * 为无可用记忆的请求创建空上下文。
     *
     * @param conversationId 即使无历史也需沿用的会话标识
     * @return 空记忆上下文
     */
    public static ConversationMemoryContext empty(String conversationId) {
        return new ConversationMemoryContext(
                ConversationMemoryConversation.empty(conversationId),
                ConversationMemorySummary.empty(),
                List.of());
    }

    private static String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 120) {
            return safeValue;
        }
        return safeValue.substring(0, 117) + "...";
    }
}


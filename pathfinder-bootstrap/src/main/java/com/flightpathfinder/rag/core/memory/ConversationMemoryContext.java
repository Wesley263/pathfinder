package com.flightpathfinder.rag.core.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 供 RAG 主链消费的记忆上下文。
 *
 * <p>该上下文有意同时携带近期轮次与可选摘要。
 * 近期轮次用于保留最新原话，摘要用于在会话超过近期窗口后为 stage one 与检索提供背景。
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
     * @return 当近期轮次与摘要都缺失时返回 {@code true}
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
     * @return 当旧轮次摘要文本存在时返回 {@code true}
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
        // 两者共同保留，避免 rewrite 与路由丢失长程上下文或即时语义细节。
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

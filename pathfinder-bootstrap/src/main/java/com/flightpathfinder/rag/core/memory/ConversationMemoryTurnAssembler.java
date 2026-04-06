package com.flightpathfinder.rag.core.memory;

import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将持久化消息行重组为“轮次级”记忆对象。
 *
 * <p>消息按行分存可保持 USER/ASSISTANT 内容可审计，
 * 而主链消费的是轮次对象。本组装器即承担从存储形态到编排形态的边界转换。
 */
final class ConversationMemoryTurnAssembler {

    private ConversationMemoryTurnAssembler() {
    }

    /**
     * 按请求与消息序将消息行分组为会话轮次。
     *
     * @param messages 单个会话的持久化消息行
     * @return 可直接用于构造记忆上下文的不可变轮次列表
     */
    static List<ConversationMemoryTurn> toTurns(List<ConversationMemoryMessageRecord> messages) {
        Map<String, TurnBuilder> grouped = new LinkedHashMap<>();
        for (ConversationMemoryMessageRecord message : messages == null ? List.<ConversationMemoryMessageRecord>of() : messages) {
            if (message == null) {
                continue;
            }
            grouped.computeIfAbsent(resolveTurnKey(message), ignored -> new TurnBuilder())
                    .accept(message);
        }

        List<ConversationMemoryTurn> turns = new ArrayList<>();
        for (Map.Entry<String, TurnBuilder> entry : grouped.entrySet()) {
            turns.add(entry.getValue().build(entry.getKey()));
        }
        return List.copyOf(turns);
    }

    private static String resolveTurnKey(ConversationMemoryMessageRecord message) {
        if (message.requestId() != null && !message.requestId().isBlank()) {
            return message.requestId();
        }
        return message.messageId();
    }

    private static final class TurnBuilder {

        private String requestId = "";
        private String userQuestion = "";
        private String rewrittenQuestion = "";
        private String answerText = "";
        private String answerStatus = "";
        private Instant createdAt;

        private void accept(ConversationMemoryMessageRecord message) {
            requestId = firstNonBlank(requestId, message.requestId());
            createdAt = createdAt == null ? message.createdAt() : createdAt;
            if ("USER".equals(message.role())) {
                userQuestion = firstNonBlank(userQuestion, message.content());
                rewrittenQuestion = firstNonBlank(rewrittenQuestion, message.rewrittenQuestion());
                return;
            }
            answerText = firstNonBlank(answerText, message.content());
            answerStatus = firstNonBlank(answerStatus, message.answerStatus());
        }

        private ConversationMemoryTurn build(String fallbackRequestId) {
            return new ConversationMemoryTurn(
                    firstNonBlank(requestId, fallbackRequestId),
                    userQuestion,
                    rewrittenQuestion,
                    answerText,
                    answerStatus,
                    createdAt);
        }

        private String firstNonBlank(String currentValue, String candidate) {
            if (currentValue != null && !currentValue.isBlank()) {
                return currentValue;
            }
            return candidate == null ? "" : candidate.trim();
        }
    }
}

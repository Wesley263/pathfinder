package com.flightpathfinder.rag.core.memory;

import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reassembles persisted message rows into turn-level memory objects.
 *
 * <p>Messages are stored separately so USER and ASSISTANT content remain auditable, but the mainline consumes
 * turns. This assembler is the small boundary that converts storage shape into orchestration shape.
 */
final class ConversationMemoryTurnAssembler {

    private ConversationMemoryTurnAssembler() {
    }

    /**
     * Groups message rows into conversation turns ordered by request/message sequence.
     *
     * @param messages persisted message rows for one conversation
     * @return immutable turn list ready for memory-context construction
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

package com.flightpathfinder.rag.core.memory;

import com.flightpathfinder.infra.ai.chat.ChatRequest;
import com.flightpathfinder.infra.ai.chat.ChatResponse;
import com.flightpathfinder.infra.ai.chat.ChatService;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryConversationRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemorySummaryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 基于阈值触发的默认摘要实现。
 *
 * 说明。
 * 同时确保长会话可以压缩为稳定上下文片段。
 * 说明。
 */
@Service
public class DefaultConversationMemorySummaryService implements ConversationMemorySummaryService {

    private final ConversationMemoryConversationRepository conversationRepository;
    private final ConversationMemoryMessageRepository messageRepository;
    private final ConversationMemorySummaryRepository summaryRepository;
    private final ConversationMemoryProperties conversationMemoryProperties;
    private final ChatService chatService;

    public DefaultConversationMemorySummaryService(
            ConversationMemoryConversationRepository conversationRepository,
            ConversationMemoryMessageRepository messageRepository,
            ConversationMemorySummaryRepository summaryRepository,
            ConversationMemoryProperties conversationMemoryProperties,
            ChatService chatService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.summaryRepository = summaryRepository;
        this.conversationMemoryProperties = conversationMemoryProperties;
        this.chatService = chatService;
    }

    @Override
    public ConversationMemorySummary loadLatestSummary(String conversationId) {
        if (conversationId == null || conversationId.isBlank() || !conversationMemoryProperties.summaryEnabled()) {
            return ConversationMemorySummary.empty();
        }
        return summaryRepository.findByConversationId(conversationId).orElse(ConversationMemorySummary.empty());
    }

    @Override
    public void refreshIfNeeded(String conversationId) {
        if (conversationId == null || conversationId.isBlank() || !conversationMemoryProperties.summaryEnabled()) {
            return;
        }
        ConversationMemoryConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElse(ConversationMemoryConversation.empty(conversationId));
        int totalTurns = conversation.turnCount();
        if (totalTurns < conversationMemoryProperties.summaryStartTurns()
                || totalTurns <= conversationMemoryProperties.recentTurnLimit()) {
            return;
        }

        List<ConversationMemoryMessageRecord> messages = messageRepository.findAllByConversationId(conversationId);
        List<ConversationMemoryTurn> turns = ConversationMemoryTurnAssembler.toTurns(messages);
        if (turns.size() < conversationMemoryProperties.summaryStartTurns()
                || turns.size() <= conversationMemoryProperties.recentTurnLimit()) {
            return;
        }

        int summarizedTurnCount = turns.size() - conversationMemoryProperties.recentTurnLimit();
        String summaryText = summarize(turns.subList(0, summarizedTurnCount));
        if (summaryText.isBlank()) {
            return;
        }
        summaryRepository.upsert(conversationId, summaryText, summarizedTurnCount);
    }

    private String summarize(List<ConversationMemoryTurn> turns) {
        String modelSummary = summarizeWithModel(turns);
        if (!modelSummary.isBlank()) {
            return abbreviate(modelSummary, conversationMemoryProperties.summaryMaxChars());
        }
        String heuristicSummary = turns.stream()
                .map(turn -> "Q: " + compressWhitespace(turn.questionForContext())
                        + " | A: " + abbreviate(compressWhitespace(turn.answerText()), 80))
                .reduce((left, right) -> left + " || " + right)
                .orElse("");
        return abbreviate(heuristicSummary, conversationMemoryProperties.summaryMaxChars());
    }

    private String summarizeWithModel(List<ConversationMemoryTurn> turns) {
        StringBuilder conversation = new StringBuilder();
        for (ConversationMemoryTurn turn : turns) {
            conversation.append("Q: ")
                    .append(compressWhitespace(turn.questionForContext()))
                    .append("\nA: ")
                    .append(compressWhitespace(turn.answerText()))
                    .append("\n\n");
        }
        ChatResponse response = chatService.chat(new ChatRequest(
                conversation.toString(),
                "Summarize the conversation in concise Chinese for future routing context. "
                        + "Keep only stable user preferences, constraints, and confirmed outcomes. "
                        + "Do not invent facts. Output plain text within "
                        + conversationMemoryProperties.summaryMaxChars()
                        + " characters.",
                Map.of("stage", "memory-summary")));
        if (response == null || response.placeholder() || response.content() == null) {
            return "";
        }
        return response.content().trim();
    }

    private String compressWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String abbreviate(String value, int maxLength) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}

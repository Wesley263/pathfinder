package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminConversationDetail;
import com.flightpathfinder.admin.service.AdminConversationDetailResult;
import com.flightpathfinder.admin.service.AdminConversationListResult;
import com.flightpathfinder.admin.service.AdminConversationMessageItem;
import com.flightpathfinder.admin.service.AdminConversationSummaryDetail;
import com.flightpathfinder.admin.service.AdminConversationSummaryItem;
import com.flightpathfinder.admin.service.AdminMemoryService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryConversation;
import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryConversationRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemorySummaryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 管理端记忆查询服务默认实现。
 *
 * <p>该服务将持久化会话记忆适配为管理端结果模型，
 * 使管理 API 能够巡检记忆状态而不直接暴露运行时记忆上下文对象。
 */
@Service
public class DefaultAdminMemoryService implements AdminMemoryService {

    private static final int DEFAULT_RECENT_MESSAGE_LIMIT = 20;

    private final ConversationMemoryConversationRepository conversationRepository;
    private final ConversationMemoryMessageRepository messageRepository;
    private final ConversationMemorySummaryRepository summaryRepository;

    public DefaultAdminMemoryService(ConversationMemoryConversationRepository conversationRepository,
                                     ConversationMemoryMessageRepository messageRepository,
                                     ConversationMemorySummaryRepository summaryRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.summaryRepository = summaryRepository;
    }

    /**
        * 列出持久化会话供管理端巡检。
     *
     * @param conversationId optional exact conversation filter
     * @param limit maximum number of conversations to return
     * @return admin-facing conversation list result
     */
    @Override
    public AdminConversationListResult listConversations(String conversationId, int limit) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<AdminConversationSummaryItem> conversations = conversationRepository.findRecent(normalizedConversationId, normalizedLimit).stream()
                .map(this::toSummaryItem)
                .toList();

        if (conversations.isEmpty()) {
            if (!normalizedConversationId.isBlank()) {
                return new AdminConversationListResult(
                        "NOT_FOUND",
                        normalizedConversationId,
                        "conversation is not available for conversationId=" + normalizedConversationId,
                        normalizedLimit,
                        0,
                        List.of());
            }
            return new AdminConversationListResult(
                    "EMPTY",
                    "",
                    "no persisted conversations are available",
                    normalizedLimit,
                    0,
                    List.of());
        }

        return new AdminConversationListResult(
                "SUCCESS",
                normalizedConversationId,
                "conversation list loaded",
                normalizedLimit,
                conversations.size(),
                    conversations);
    }

    /**
        * 加载单个持久化会话详情视图。
     *
     * @param conversationId exact conversation id
     * @param recentMessageLimit maximum number of recent messages to include
     * @return admin-facing conversation detail result
     */
    @Override
    public AdminConversationDetailResult findConversationDetail(String conversationId, int recentMessageLimit) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (normalizedConversationId.isBlank()) {
            return new AdminConversationDetailResult(
                    "",
                    "NOT_FOUND",
                    "conversationId is required",
                    null);
        }

        Optional<ConversationMemoryConversation> conversation = conversationRepository.findByConversationId(normalizedConversationId);
        if (conversation.isEmpty()) {
            return new AdminConversationDetailResult(
                    normalizedConversationId,
                    "NOT_FOUND",
                    "conversation is not available for conversationId=" + normalizedConversationId,
                    null);
        }

        ConversationMemorySummary summary = summaryRepository.findByConversationId(normalizedConversationId)
                .orElse(ConversationMemorySummary.empty());
        // 详情中分离展示最近原始消息与摘要状态，便于判断是否已生成摘要以及仍保留多少显式消息。
        List<AdminConversationMessageItem> recentMessages = messageRepository.findRecentByConversationId(
                        normalizedConversationId,
                        normalizeRecentMessageLimit(recentMessageLimit))
                .stream()
                .map(this::toMessageItem)
                .toList();

        return new AdminConversationDetailResult(
                normalizedConversationId,
                "SUCCESS",
                "conversation detail loaded",
                new AdminConversationDetail(
                        conversation.get().conversationId(),
                        conversation.get().lastRequestId(),
                        conversation.get().turnCount(),
                        conversation.get().messageCount(),
                        conversation.get().createdAt(),
                        conversation.get().updatedAt(),
                        summary.exists(),
                        new AdminConversationSummaryDetail(
                                summary.exists(),
                                summary.summaryText(),
                                summary.summarizedTurnCount(),
                                summary.updatedAt()),
                        recentMessages));
    }

    private AdminConversationSummaryItem toSummaryItem(ConversationMemoryConversation conversation) {
        ConversationMemorySummary summary = summaryRepository.findByConversationId(conversation.conversationId())
                .orElse(ConversationMemorySummary.empty());
        return new AdminConversationSummaryItem(
                conversation.conversationId(),
                conversation.lastRequestId(),
                conversation.turnCount(),
                conversation.messageCount(),
                summary.exists(),
                summary.updatedAt(),
                conversation.createdAt(),
                conversation.updatedAt());
    }

    private AdminConversationMessageItem toMessageItem(ConversationMemoryMessageRecord messageRecord) {
        return new AdminConversationMessageItem(
                messageRecord.messageId(),
                messageRecord.requestId(),
                messageRecord.messageIndex(),
                messageRecord.role(),
                messageRecord.content(),
                messageRecord.rewrittenQuestion(),
                messageRecord.answerStatus(),
                messageRecord.createdAt());
    }

    private String normalizeConversationId(String conversationId) {
        return conversationId == null ? "" : conversationId.trim();
    }

    private int normalizeRecentMessageLimit(int recentMessageLimit) {
        if (recentMessageLimit <= 0) {
            return DEFAULT_RECENT_MESSAGE_LIMIT;
        }
        return Math.min(recentMessageLimit, 100);
    }
}

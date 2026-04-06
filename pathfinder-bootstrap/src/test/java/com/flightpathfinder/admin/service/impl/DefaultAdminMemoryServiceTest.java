package com.flightpathfinder.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flightpathfinder.admin.service.AdminConversationDetailResult;
import com.flightpathfinder.admin.service.AdminConversationListResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryConversation;
import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryConversationRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemorySummaryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */

@ExtendWith(MockitoExtension.class)
class DefaultAdminMemoryServiceTest {

    @Mock
    private ConversationMemoryConversationRepository conversationRepository;

    @Mock
    private ConversationMemoryMessageRepository messageRepository;

    @Mock
    private ConversationMemorySummaryRepository summaryRepository;

    private DefaultAdminMemoryService adminMemoryService;

    @BeforeEach
    void setUp() {
        adminMemoryService = new DefaultAdminMemoryService(
                conversationRepository,
                messageRepository,
                summaryRepository);
    }

    @Test
    void listConversations_shouldReturnEmptyWhenNoConversationAndNoFilter() {
        when(conversationRepository.findRecent("", 10)).thenReturn(List.of());

        AdminConversationListResult result = adminMemoryService.listConversations(null, 10);

        assertEquals("EMPTY", result.status());
        assertEquals("", result.requestedConversationId());
        assertEquals(0, result.count());
        assertTrue(result.message().contains("no persisted conversations"));
        assertTrue(result.conversations().isEmpty());
    }

    @Test
    void listConversations_shouldReturnNotFoundWhenFilterSpecifiedButMissing() {
        when(conversationRepository.findRecent("conv-x", 5)).thenReturn(List.of());

        AdminConversationListResult result = adminMemoryService.listConversations("conv-x", 5);

        assertEquals("NOT_FOUND", result.status());
        assertEquals("conv-x", result.requestedConversationId());
        assertTrue(result.message().contains("conv-x"));
        assertTrue(result.conversations().isEmpty());
    }

    @Test
    void listConversations_shouldMapSummaryAndNormalizeLimit() {
        ConversationMemoryConversation conversation = new ConversationMemoryConversation(
                "conv-1",
                "req-1",
                3,
                6,
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:10:00Z"));
        when(conversationRepository.findRecent("", 100)).thenReturn(List.of(conversation));
        when(summaryRepository.findByConversationId("conv-1"))
                .thenReturn(Optional.of(new ConversationMemorySummary("summary", 2, Instant.parse("2026-04-06T10:05:00Z"))));

        AdminConversationListResult result = adminMemoryService.listConversations("", 200);

        assertEquals("SUCCESS", result.status());
        assertEquals(100, result.limit());
        assertEquals(1, result.count());
        assertEquals("conv-1", result.conversations().getFirst().conversationId());
        assertTrue(result.conversations().getFirst().hasSummary());
    }

    @Test
    void findConversationDetail_shouldReturnSuccessWithMessagesAndSummary() {
        ConversationMemoryConversation conversation = new ConversationMemoryConversation(
                "conv-1",
                "req-2",
                2,
                4,
                Instant.parse("2026-04-06T09:00:00Z"),
                Instant.parse("2026-04-06T09:10:00Z"));
        ConversationMemoryMessageRecord messageRecord = new ConversationMemoryMessageRecord(
                "msg-1",
                "conv-1",
                "req-2",
                2,
                "ASSISTANT",
                "回答内容",
                "改写问题",
                "SUCCESS",
                Instant.parse("2026-04-06T09:10:00Z"));
        when(conversationRepository.findByConversationId("conv-1")).thenReturn(Optional.of(conversation));
        when(summaryRepository.findByConversationId("conv-1"))
                .thenReturn(Optional.of(new ConversationMemorySummary("summary", 1, Instant.parse("2026-04-06T09:05:00Z"))));
        when(messageRepository.findRecentByConversationId("conv-1", 20)).thenReturn(List.of(messageRecord));

        AdminConversationDetailResult result = adminMemoryService.findConversationDetail("conv-1", 0);

        assertEquals("SUCCESS", result.status());
        assertEquals("conv-1", result.conversationId());
        assertTrue(result.detail().hasSummary());
        assertEquals(1, result.detail().summary().summarizedTurnCount());
        assertEquals(1, result.detail().recentMessages().size());
        assertEquals("msg-1", result.detail().recentMessages().getFirst().messageId());
        verify(messageRepository).findRecentByConversationId("conv-1", 20);
    }

    @Test
    void findConversationDetail_shouldReturnNotFoundWhenConversationIdBlank() {
        AdminConversationDetailResult result = adminMemoryService.findConversationDetail("  ", 10);

        assertEquals("NOT_FOUND", result.status());
        assertFalse(result.message().isBlank());
        assertEquals(null, result.detail());
    }
}




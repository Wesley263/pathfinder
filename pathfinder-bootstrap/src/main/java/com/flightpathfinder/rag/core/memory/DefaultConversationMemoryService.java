package com.flightpathfinder.rag.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 记忆加载与写入流程的默认编排实现。
 *
 * <p>该层将记忆生命周期职责从 stage-one、检索与回答服务中隔离出来，
 * 并将近期轮次存储与可选摘要加载统一组合，使主链仅消费单一记忆上下文对象。
 */
@Service
public class DefaultConversationMemoryService implements ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultConversationMemoryService.class);

    private final ConversationMemoryStore conversationMemoryStore;
    private final ConversationMemorySummaryService conversationMemorySummaryService;
    private final ConversationMemoryProperties conversationMemoryProperties;

    public DefaultConversationMemoryService(ConversationMemoryStore conversationMemoryStore,
                                            ConversationMemorySummaryService conversationMemorySummaryService,
                                            ConversationMemoryProperties conversationMemoryProperties) {
        this.conversationMemoryStore = conversationMemoryStore;
        this.conversationMemorySummaryService = conversationMemorySummaryService;
        this.conversationMemoryProperties = conversationMemoryProperties;
    }

    /**
     * 加载当前请求会话的记忆上下文。
     *
     * @param conversationId 当前查询携带的会话标识；为空表示本次请求不使用记忆
     * @return 标识为空或无历史时返回空上下文，否则返回近期轮次与可选摘要
     */
    @Override
    public ConversationMemoryContext loadContext(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return ConversationMemoryContext.empty("");
        }
        ConversationMemorySnapshot snapshot = conversationMemoryStore.load(
                conversationId,
                conversationMemoryProperties.recentTurnLimit());
        ConversationMemorySummary summary;
        try {
            summary = conversationMemorySummaryService.loadLatestSummary(conversationId);
        } catch (RuntimeException exception) {
            log.warn("Failed to load conversation summary for conversationId={}", conversationId, exception);
            summary = ConversationMemorySummary.empty();
        }
        // 近期轮次与摘要独立加载，
        // 这样摘要失败可降级，不会阻断整条查询路径。
        if (snapshot.turns().isEmpty() && !summary.exists()) {
            return ConversationMemoryContext.empty(conversationId);
        }
        return new ConversationMemoryContext(snapshot.conversation(), summary, snapshot.turns());
    }

    /**
     * 持久化已完成轮次，并在需要时刷新摘要状态。
     *
     * @param writeRequest 从当前查询生命周期产出的完成轮次负载
     */
    @Override
    public void appendTurn(ConversationMemoryWriteRequest writeRequest) {
        if (writeRequest == null || writeRequest.conversationId().isBlank()) {
            return;
        }
        conversationMemoryStore.appendTurn(
                writeRequest.conversationId(),
                new ConversationMemoryTurn(
                        writeRequest.requestId(),
                        writeRequest.userQuestion(),
                        writeRequest.rewrittenQuestion(),
                        writeRequest.answerText(),
                        writeRequest.answerStatus(),
                        writeRequest.createdAt()));
        try {
            // 摘要刷新由阈值驱动且采用尽力而为策略：
            // 压缩失败不能阻塞刚完成轮次的主写路径。
            conversationMemorySummaryService.refreshIfNeeded(writeRequest.conversationId());
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh conversation summary for conversationId={}", writeRequest.conversationId(), exception);
        }
    }
}

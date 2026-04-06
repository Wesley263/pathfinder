package com.flightpathfinder.rag.core.memory;

import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryConversationRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 JDBC 的会话记忆存储实现。
 *
 * 负责会话元信息与轮次消息的读取、组装与持久化，
 * 会话、消息、摘要三表分离后，可在引入摘要压缩后依然保持原始轮次可读与可审计。
 */
@Repository
public class JdbcConversationMemoryStore implements ConversationMemoryStore {

    private final ConversationMemoryConversationRepository conversationRepository;
    private final ConversationMemoryMessageRepository messageRepository;

    public JdbcConversationMemoryStore(
            ConversationMemoryConversationRepository conversationRepository,
            ConversationMemoryMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 加载会话元信息及近期持久化轮次。
     *
     * @param conversationId 稳定会话标识
     * @param recentTurnLimit 需物化到快照中的近期轮次数量
     * @return 近期轮次快照；会话尚不存在时返回空快照
     */
    @Override
    public ConversationMemorySnapshot load(String conversationId, int recentTurnLimit) {
        if (conversationId == null || conversationId.isBlank()) {
            return ConversationMemorySnapshot.empty("");
        }
        ConversationMemoryConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElse(ConversationMemoryConversation.empty(conversationId));
        if (conversation.turnCount() == 0) {
            return ConversationMemorySnapshot.empty(conversationId);
        }

        // 消息按行存储，再重组为轮次，
        // 以适配存储模型与应用层轮次模型之间的差异。
        List<ConversationMemoryMessageRecord> messages =
                messageRepository.findRecentByConversationId(conversationId, Math.max(1, recentTurnLimit) * 2);
        List<ConversationMemoryTurn> turns = ConversationMemoryTurnAssembler.toTurns(messages);
        return new ConversationMemorySnapshot(conversation, turns);
    }

        /**
         * 持久化一轮完成对话并更新会话级计数。
         *
         * @param conversationId 稳定会话标识
         * @param turn 完整用户/助手轮次
         */
    @Override
    @Transactional
    public void appendTurn(String conversationId, ConversationMemoryTurn turn) {
        if (conversationId == null || conversationId.isBlank() || turn == null) {
            return;
        }
        ConversationMemoryTurn safeTurn = ensureRequestId(turn);
        conversationRepository.upsertAfterTurn(conversationId, safeTurn.requestId());
        messageRepository.saveTurn(conversationId, safeTurn);
    }

    private ConversationMemoryTurn ensureRequestId(ConversationMemoryTurn turn) {
        if (turn.requestId() != null && !turn.requestId().isBlank()) {
            return turn;
        }
        // 写入层补齐 requestId，保证后续追踪和审计链路可关联。
        // 因此当调用方缺失时由存储层补齐。
        return new ConversationMemoryTurn(
                UUID.randomUUID().toString(),
                turn.userQuestion(),
                turn.rewrittenQuestion(),
                turn.answerText(),
                turn.answerStatus(),
                turn.createdAt());
    }
}


package com.flightpathfinder.rag.core.memory;

import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryConversationRepository;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRecord;
import com.flightpathfinder.rag.core.memory.repository.ConversationMemoryMessageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-backed recent-turn store for conversation memory.
 *
 * <p>This implementation coordinates conversation metadata and message persistence but deliberately does not
 * own summary generation. The split between conversation, message and summary tables keeps auditability high:
 * raw turns remain readable even after summary compaction is introduced.
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
     * Loads conversation metadata plus recent persisted turns.
     *
     * @param conversationId stable conversation identifier
     * @param recentTurnLimit number of recent turns to materialize into the snapshot
     * @return recent-turn snapshot, or empty when the conversation does not exist yet
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

        // Messages are stored row-by-row, then reassembled into turns so the persistence model can keep both
        // USER and ASSISTANT messages explicit.
        List<ConversationMemoryMessageRecord> messages =
                messageRepository.findRecentByConversationId(conversationId, Math.max(1, recentTurnLimit) * 2);
        List<ConversationMemoryTurn> turns = ConversationMemoryTurnAssembler.toTurns(messages);
        return new ConversationMemorySnapshot(conversation, turns);
    }

    /**
     * Persists one completed turn and updates conversation-level counters.
     *
     * @param conversationId stable conversation identifier
     * @param turn completed user/assistant turn
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
        // requestId is the cross-cutting join key between conversation messages and trace/audit facts, so the
        // store backfills one if the caller omitted it.
        return new ConversationMemoryTurn(
                UUID.randomUUID().toString(),
                turn.userQuestion(),
                turn.rewrittenQuestion(),
                turn.answerText(),
                turn.answerStatus(),
                turn.createdAt());
    }
}

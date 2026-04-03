package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryTurn;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for raw conversation message rows.
 *
 * <p>One logical turn is written as two rows so user text, rewritten question and assistant answer remain
 * independently auditable.
 */
@Repository
public class JdbcConversationMemoryMessageRepository implements ConversationMemoryMessageRepository {

    private static final String INSERT_SQL = """
            INSERT INTO t_rag_conversation_message (
                message_id,
                conversation_id,
                request_id,
                message_index,
                role,
                content,
                rewritten_question,
                answer_status,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_RECENT_SQL = """
            SELECT message_id, conversation_id, request_id, message_index, role, content, rewritten_question, answer_status, created_at
            FROM t_rag_conversation_message
            WHERE conversation_id = ?
            ORDER BY created_at DESC, message_index DESC
            LIMIT ?
            """;

    private static final String SELECT_ALL_SQL = """
            SELECT message_id, conversation_id, request_id, message_index, role, content, rewritten_question, answer_status, created_at
            FROM t_rag_conversation_message
            WHERE conversation_id = ?
            ORDER BY created_at ASC, message_index ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationMemoryMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists a completed turn as one USER row and one ASSISTANT row.
     *
     * @param conversationId stable conversation identifier
     * @param turn completed turn to persist
     */
    @Override
    public void saveTurn(String conversationId, ConversationMemoryTurn turn) {
        Timestamp createdAt = Timestamp.from(turn.createdAt());
        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                List.of(
                        new Object[]{
                                UUID.randomUUID().toString(),
                                conversationId,
                                turn.requestId(),
                                1,
                                "USER",
                                turn.userQuestion(),
                                turn.rewrittenQuestion(),
                                "",
                                createdAt
                        },
                        new Object[]{
                                UUID.randomUUID().toString(),
                                conversationId,
                                turn.requestId(),
                                2,
                                "ASSISTANT",
                                turn.answerText(),
                                turn.rewrittenQuestion(),
                                turn.answerStatus(),
                                createdAt
                        }
                ));
    }

    /**
     * Loads recent message rows for a conversation.
     *
     * @param conversationId stable conversation identifier
     * @param limit maximum number of message rows to return
     * @return recent rows reordered oldest-to-newest for turn assembly
     */
    @Override
    public List<ConversationMemoryMessageRecord> findRecentByConversationId(String conversationId, int limit) {
        List<ConversationMemoryMessageRecord> rows = jdbcTemplate.query(
                SELECT_RECENT_SQL,
                new MessageRowMapper(),
                conversationId,
                Math.max(1, limit));
        // SQL fetches newest-first for index efficiency; the assembler expects chronological order.
        return rows.reversed();
    }

    /**
     * Loads the full message history for a conversation.
     *
     * @param conversationId stable conversation identifier
     * @return full chronological message history
     */
    @Override
    public List<ConversationMemoryMessageRecord> findAllByConversationId(String conversationId) {
        return jdbcTemplate.query(SELECT_ALL_SQL, new MessageRowMapper(), conversationId);
    }

    private static final class MessageRowMapper implements RowMapper<ConversationMemoryMessageRecord> {

        @Override
        public ConversationMemoryMessageRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new ConversationMemoryMessageRecord(
                    rs.getString("message_id"),
                    rs.getString("conversation_id"),
                    rs.getString("request_id"),
                    rs.getInt("message_index"),
                    rs.getString("role"),
                    rs.getString("content"),
                    rs.getString("rewritten_question"),
                    rs.getString("answer_status"),
                    createdAt == null ? null : createdAt.toInstant());
        }
    }
}

package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemoryConversation;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for conversation-level memory headers.
 *
 * <p>This repository keeps conversation counters and timestamps separate from individual messages so list and
 * summary queries can stay lightweight.
 */
@Repository
public class JdbcConversationMemoryConversationRepository implements ConversationMemoryConversationRepository {

    private static final String SELECT_SQL = """
            SELECT conversation_id, last_request_id, turn_count, message_count, created_at, updated_at
            FROM t_rag_conversation
            WHERE conversation_id = ?
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO t_rag_conversation (
                conversation_id,
                last_request_id,
                turn_count,
                message_count,
                created_at,
                updated_at
            )
            VALUES (?, ?, 1, 2, ?, ?)
            ON CONFLICT (conversation_id)
            DO UPDATE SET
                last_request_id = EXCLUDED.last_request_id,
                turn_count = t_rag_conversation.turn_count + 1,
                message_count = t_rag_conversation.message_count + 2,
                updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationMemoryConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds one conversation header by conversation id.
     *
     * @param conversationId stable conversation identifier
     * @return conversation metadata when present
     */
    @Override
    public Optional<ConversationMemoryConversation> findByConversationId(String conversationId) {
        return jdbcTemplate.query(SELECT_SQL, new ConversationRowMapper(), conversationId)
                .stream()
                .findFirst();
    }

    /**
     * Lists recent conversations with an optional exact id filter.
     *
     * @param conversationId optional exact filter
     * @param limit maximum result size
     * @return recent conversation headers sorted by update time
     */
    @Override
    public List<ConversationMemoryConversation> findRecent(String conversationId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT conversation_id, last_request_id, turn_count, message_count, created_at, updated_at
                FROM t_rag_conversation
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (conversationId != null && !conversationId.isBlank()) {
            sql.append(" AND conversation_id = ?");
            args.add(conversationId.trim());
        }
        sql.append(" ORDER BY updated_at DESC, created_at DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 100)));
        return jdbcTemplate.query(sql.toString(), new ConversationRowMapper(), args.toArray());
    }

    /**
     * Upserts the conversation header after one completed turn has been stored.
     *
     * @param conversationId stable conversation identifier
     * @param requestId latest request id for the conversation
     */
    @Override
    public void upsertAfterTurn(String conversationId, String requestId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                UPSERT_SQL,
                conversationId,
                requestId == null ? "" : requestId.trim(),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private static final class ConversationRowMapper implements RowMapper<ConversationMemoryConversation> {

        @Override
        public ConversationMemoryConversation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ConversationMemoryConversation(
                    rs.getString("conversation_id"),
                    rs.getString("last_request_id"),
                    rs.getInt("turn_count"),
                    rs.getInt("message_count"),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}

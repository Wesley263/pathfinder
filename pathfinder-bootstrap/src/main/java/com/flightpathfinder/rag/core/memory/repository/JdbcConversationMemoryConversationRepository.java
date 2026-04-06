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
 * 会话头信息仓储 JDBC 实现。
 *
 * 负责会话级统计与最近请求标识维护，
 * 使列表与摘要查询保持轻量。
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
     * 按会话标识查询单条会话头信息。
     *
     * @param conversationId 稳定会话标识
     * @return 命中时返回会话元数据
     */
    @Override
    public Optional<ConversationMemoryConversation> findByConversationId(String conversationId) {
        return jdbcTemplate.query(SELECT_SQL, new ConversationRowMapper(), conversationId)
                .stream()
                .findFirst();
    }

    /**
     * 查询近期会话，并支持按会话标识精确过滤。
     *
     * @param conversationId 可选精确过滤值
     * @param limit 最大结果数量
     * @return 按更新时间排序的会话头列表
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
     * 在单轮对话落库后更新或插入会话头信息。
     *
     * @param conversationId 稳定会话标识
     * @param requestId 会话最近请求标识
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


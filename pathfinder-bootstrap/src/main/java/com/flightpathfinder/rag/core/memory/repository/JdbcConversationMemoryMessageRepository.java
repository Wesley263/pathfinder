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
 * 说明。
 *
 * 说明。
 * 以保持用户文本、改写问题与助手回答可独立审计。
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
         * 说明。
         *
         * @param conversationId 稳定会话标识
         * @param turn 待持久化的完成轮次
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
         * 加载会话近期消息记录。
         *
         * @param conversationId 稳定会话标识
         * @param limit 最大返回消息行数
         * @return 按“由旧到新”重排后的近期记录，便于轮次组装
         */
    @Override
    public List<ConversationMemoryMessageRecord> findRecentByConversationId(String conversationId, int limit) {
        List<ConversationMemoryMessageRecord> rows = jdbcTemplate.query(
                SELECT_RECENT_SQL,
                new MessageRowMapper(),
                conversationId,
                Math.max(1, limit));
                // 查询语句为索引效率按“新到旧”拉取；组装器需要时间正序，因此这里反转。
        return rows.reversed();
    }

        /**
         * 加载会话完整消息历史。
         *
         * @param conversationId 稳定会话标识
         * @return 完整时间序消息历史
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

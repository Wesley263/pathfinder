package com.flightpathfinder.rag.core.memory.repository;

import com.flightpathfinder.rag.core.memory.ConversationMemorySummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 说明。
 *
 * 说明。
 * 以便刷新压缩结果时无需改写底层消息档案。
 */
@Repository
public class JdbcConversationMemorySummaryRepository implements ConversationMemorySummaryRepository {

    private static final String SELECT_SQL = """
            SELECT conversation_id, summary_text, summarized_turn_count, updated_at
            FROM t_rag_conversation_summary
            WHERE conversation_id = ?
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO t_rag_conversation_summary (
                conversation_id,
                summary_text,
                summarized_turn_count,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (conversation_id)
            DO UPDATE SET
                summary_text = EXCLUDED.summary_text,
                summarized_turn_count = EXCLUDED.summarized_turn_count,
                updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationMemorySummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询指定会话的持久化摘要。
     *
     * @param conversationId 稳定会话标识
     * @return 命中时返回摘要
     */
    @Override
    public Optional<ConversationMemorySummary> findByConversationId(String conversationId) {
        return jdbcTemplate.query(SELECT_SQL, new SummaryRowMapper(), conversationId)
                .stream()
                .findFirst();
    }

    /**
     * 为会话插入或更新摘要行。
     *
     * @param conversationId 稳定会话标识
     * @param summaryText 压缩后的摘要文本
     * @param summarizedTurnCount 摘要已覆盖的历史轮次数
     */
    @Override
    public void upsert(String conversationId, String summaryText, int summarizedTurnCount) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                UPSERT_SQL,
                conversationId,
                summaryText,
                summarizedTurnCount,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private static final class SummaryRowMapper implements RowMapper<ConversationMemorySummary> {

        @Override
        public ConversationMemorySummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return new ConversationMemorySummary(
                    rs.getString("summary_text"),
                    rs.getInt("summarized_turn_count"),
                    updatedAt == null ? null : updatedAt.toInstant());
        }
    }
}

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
 * JDBC repository for persisted conversation summaries.
 *
 * <p>Summary rows are stored independently from raw history so compaction can be refreshed without rewriting
 * the underlying message archive.
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
     * Finds the persisted summary for the given conversation.
     *
     * @param conversationId stable conversation identifier
     * @return persisted summary when present
     */
    @Override
    public Optional<ConversationMemorySummary> findByConversationId(String conversationId) {
        return jdbcTemplate.query(SELECT_SQL, new SummaryRowMapper(), conversationId)
                .stream()
                .findFirst();
    }

    /**
     * Inserts or updates the summary row for the conversation.
     *
     * @param conversationId stable conversation identifier
     * @param summaryText compacted summary text
     * @param summarizedTurnCount number of historical turns covered by the summary
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

package com.flightpathfinder.rag.core.trace.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for trace-run headers.
 *
 * <p>This repository supports both direct detail lookup by trace id and recent-run listing by request or
 * conversation identifiers.
 */
@Repository
public class JdbcRagTraceRunRepository implements RagTraceRunRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO t_rag_trace_run (
                trace_id,
                request_id,
                conversation_id,
                scene,
                overall_status,
                snapshot_miss_occurred,
                started_at,
                finished_at,
                node_count,
                tool_count,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (trace_id)
            DO UPDATE SET
                request_id = EXCLUDED.request_id,
                conversation_id = EXCLUDED.conversation_id,
                scene = EXCLUDED.scene,
                overall_status = EXCLUDED.overall_status,
                snapshot_miss_occurred = EXCLUDED.snapshot_miss_occurred,
                started_at = EXCLUDED.started_at,
                finished_at = EXCLUDED.finished_at,
                node_count = EXCLUDED.node_count,
                tool_count = EXCLUDED.tool_count,
                updated_at = EXCLUDED.updated_at
            """;

    private static final String SELECT_BY_TRACE_ID_SQL = """
            SELECT trace_id, request_id, conversation_id, scene, overall_status, snapshot_miss_occurred,
                   started_at, finished_at, node_count, tool_count
            FROM t_rag_trace_run
            WHERE trace_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRagTraceRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts or updates a trace-run header row.
     *
     * @param record persisted run record for one request
     */
    @Override
    public void upsert(PersistedRagTraceRunRecord record) {
        Timestamp now = Timestamp.from(record.finishedAt());
        jdbcTemplate.update(
                UPSERT_SQL,
                record.traceId(),
                record.requestId(),
                record.conversationId(),
                record.scene(),
                record.overallStatus(),
                record.snapshotMissOccurred(),
                Timestamp.from(record.startedAt()),
                Timestamp.from(record.finishedAt()),
                record.nodeCount(),
                record.toolCount(),
                now,
                now);
    }

    /**
     * Finds one trace-run header by trace id.
     *
     * @param traceId unique trace identifier
     * @return run record when found
     */
    @Override
    public Optional<PersistedRagTraceRunRecord> findByTraceId(String traceId) {
        return jdbcTemplate.query(SELECT_BY_TRACE_ID_SQL, new RunRowMapper(), traceId).stream().findFirst();
    }

    /**
     * Lists recent trace-run headers filtered by request id or conversation id.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of runs to return
     * @return recent run records ordered newest-first
     */
    @Override
    public List<PersistedRagTraceRunRecord> findRecent(String requestId, String conversationId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT trace_id, request_id, conversation_id, scene, overall_status, snapshot_miss_occurred,
                       started_at, finished_at, node_count, tool_count
                FROM t_rag_trace_run
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (requestId != null && !requestId.isBlank()) {
            sql.append(" AND request_id = ?");
            args.add(requestId.trim());
        }
        if (conversationId != null && !conversationId.isBlank()) {
            sql.append(" AND conversation_id = ?");
            args.add(conversationId.trim());
        }
        sql.append(" ORDER BY started_at DESC, finished_at DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 100)));
        return jdbcTemplate.query(sql.toString(), new RunRowMapper(), args.toArray());
    }

    private static final class RunRowMapper implements RowMapper<PersistedRagTraceRunRecord> {

        @Override
        public PersistedRagTraceRunRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PersistedRagTraceRunRecord(
                    rs.getString("trace_id"),
                    rs.getString("request_id"),
                    rs.getString("conversation_id"),
                    rs.getString("scene"),
                    rs.getString("overall_status"),
                    rs.getBoolean("snapshot_miss_occurred"),
                    rs.getTimestamp("started_at").toInstant(),
                    rs.getTimestamp("finished_at").toInstant(),
                    rs.getInt("node_count"),
                    rs.getInt("tool_count"));
        }
    }
}

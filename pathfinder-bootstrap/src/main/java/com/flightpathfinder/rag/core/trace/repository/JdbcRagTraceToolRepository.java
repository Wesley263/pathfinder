package com.flightpathfinder.rag.core.trace.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC repository for persisted MCP tool summaries inside one trace.
 *
 * <p>Tool summaries are stored separately so snapshot miss, partial outcomes and other MCP-specific facts can
 * be queried directly without unpacking retrieval node JSON.
 */
@Repository
public class JdbcRagTraceToolRepository implements RagTraceToolRepository {

    private static final String DELETE_SQL = "DELETE FROM t_rag_trace_tool WHERE trace_id = ?";
    private static final String INSERT_SQL = """
            INSERT INTO t_rag_trace_tool (
                trace_id,
                tool_index,
                tool_id,
                status,
                message,
                snapshot_miss,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_SQL = """
            SELECT trace_id, tool_index, tool_id, status, message, snapshot_miss
            FROM t_rag_trace_tool
            WHERE trace_id = ?
            ORDER BY tool_index ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRagTraceToolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Replaces all tool summary rows for the given trace id.
     *
     * @param traceId unique trace identifier
     * @param records full ordered tool summary set
     */
    @Override
    @Transactional
    public void replaceForTrace(String traceId, List<PersistedRagTraceToolRecord> records) {
        jdbcTemplate.update(DELETE_SQL, traceId);
        if (records == null || records.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                records,
                records.size(),
                (ps, record) -> {
                    ps.setString(1, record.traceId());
                    ps.setInt(2, record.toolIndex());
                    ps.setString(3, record.toolId());
                    ps.setString(4, record.status());
                    ps.setString(5, record.message());
                    ps.setBoolean(6, record.snapshotMiss());
                    ps.setTimestamp(7, Timestamp.from(Instant.now()));
                });
    }

    /**
     * Loads all tool summary rows for one trace.
     *
     * @param traceId unique trace identifier
     * @return tool summary rows ordered by tool index
     */
    @Override
    public List<PersistedRagTraceToolRecord> findByTraceId(String traceId) {
        return jdbcTemplate.query(SELECT_SQL, new ToolRowMapper(), traceId);
    }

    private static final class ToolRowMapper implements RowMapper<PersistedRagTraceToolRecord> {

        @Override
        public PersistedRagTraceToolRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PersistedRagTraceToolRecord(
                    rs.getString("trace_id"),
                    rs.getInt("tool_index"),
                    rs.getString("tool_id"),
                    rs.getString("status"),
                    rs.getString("message"),
                    rs.getBoolean("snapshot_miss"));
        }
    }
}

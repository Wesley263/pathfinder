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
 * JDBC repository for trace nodes.
 *
 * <p>Nodes are replaced as a full ordered set once a trace finishes, which keeps persistence aligned with the
 * immutable "finished trace" mental model.
 */
@Repository
public class JdbcRagTraceNodeRepository implements RagTraceNodeRepository {

    private static final String DELETE_SQL = "DELETE FROM t_rag_trace_node WHERE trace_id = ?";
    private static final String INSERT_SQL = """
            INSERT INTO t_rag_trace_node (
                trace_id,
                node_index,
                node_name,
                node_type,
                status,
                summary,
                started_at,
                finished_at,
                attributes_json,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_SQL = """
            SELECT trace_id, node_index, node_name, node_type, status, summary, started_at, finished_at, attributes_json
            FROM t_rag_trace_node
            WHERE trace_id = ?
            ORDER BY node_index ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRagTraceNodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Replaces all node rows for the given trace id.
     *
     * @param traceId unique trace identifier
     * @param records full ordered node record set
     */
    @Override
    @Transactional
    public void replaceForTrace(String traceId, List<PersistedRagTraceNodeRecord> records) {
        jdbcTemplate.update(DELETE_SQL, traceId);
        if (records == null || records.isEmpty()) {
            return;
        }
        // Trace nodes are rewritten as a batch at finish time so the stored timeline matches the finalized
        // request view exactly.
        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                records,
                records.size(),
                (ps, record) -> {
                    Instant createdAt = record.finishedAt() == null ? Instant.now() : record.finishedAt();
                    ps.setString(1, record.traceId());
                    ps.setInt(2, record.nodeIndex());
                    ps.setString(3, record.nodeName());
                    ps.setString(4, record.nodeType());
                    ps.setString(5, record.status());
                    ps.setString(6, record.summary());
                    ps.setTimestamp(7, Timestamp.from(record.startedAt()));
                    ps.setTimestamp(8, Timestamp.from(record.finishedAt()));
                    ps.setString(9, record.attributesJson());
                    ps.setTimestamp(10, Timestamp.from(createdAt));
                });
    }

    /**
     * Loads node rows for one trace.
     *
     * @param traceId unique trace identifier
     * @return node records ordered by node index
     */
    @Override
    public List<PersistedRagTraceNodeRecord> findByTraceId(String traceId) {
        return jdbcTemplate.query(SELECT_SQL, new NodeRowMapper(), traceId);
    }

    private static final class NodeRowMapper implements RowMapper<PersistedRagTraceNodeRecord> {

        @Override
        public PersistedRagTraceNodeRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PersistedRagTraceNodeRecord(
                    rs.getString("trace_id"),
                    rs.getInt("node_index"),
                    rs.getString("node_name"),
                    rs.getString("node_type"),
                    rs.getString("status"),
                    rs.getString("summary"),
                    rs.getTimestamp("started_at").toInstant(),
                    rs.getTimestamp("finished_at").toInstant(),
                    rs.getString("attributes_json"));
        }
    }
}

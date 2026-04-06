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
 * 跟踪运行头的 JDBC 仓储实现。
 *
 * <p>该仓储同时支持按 traceId 直查详情入口，
 * 以及按 requestId / conversationId 维度列出近期运行记录。
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
     * 插入或更新一条 trace 运行头记录。
     *
     * @param record 单次请求对应的持久化运行记录
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
     * 按 traceId 查询单条 trace 运行头记录。
     *
     * @param traceId 唯一 trace 标识
     * @return 命中时返回运行记录
     */
    @Override
    public Optional<PersistedRagTraceRunRecord> findByTraceId(String traceId) {
        return jdbcTemplate.query(SELECT_BY_TRACE_ID_SQL, new RunRowMapper(), traceId).stream().findFirst();
    }

    /**
     * 按请求标识或会话标识过滤并列出近期 trace 运行头。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 按时间倒序排列的近期运行记录
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

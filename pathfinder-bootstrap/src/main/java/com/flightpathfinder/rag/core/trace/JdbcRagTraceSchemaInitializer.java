package com.flightpathfinder.rag.core.trace;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Rag 追踪表结构初始化器。
 *
 * 在应用启动阶段确保 run、node、tool 三张追踪表及索引存在。
 */
@Component
public class JdbcRagTraceSchemaInitializer {

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS t_rag_trace_run (
                trace_id VARCHAR(64) PRIMARY KEY,
                request_id VARCHAR(128) NOT NULL,
                conversation_id VARCHAR(128) NOT NULL DEFAULT '',
                scene VARCHAR(128) NOT NULL,
                overall_status VARCHAR(64) NOT NULL,
                snapshot_miss_occurred BOOLEAN NOT NULL DEFAULT FALSE,
                started_at TIMESTAMPTZ NOT NULL,
                finished_at TIMESTAMPTZ NOT NULL,
                node_count INTEGER NOT NULL DEFAULT 0,
                tool_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_trace_run_request_id
                ON t_rag_trace_run (request_id, started_at DESC)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_trace_run_conversation_id
                ON t_rag_trace_run (conversation_id, started_at DESC)
            """,
            """
            CREATE TABLE IF NOT EXISTS t_rag_trace_node (
                id BIGSERIAL PRIMARY KEY,
                trace_id VARCHAR(64) NOT NULL,
                node_index INTEGER NOT NULL,
                node_name VARCHAR(64) NOT NULL,
                node_type VARCHAR(32) NOT NULL,
                status VARCHAR(64) NOT NULL,
                summary TEXT NOT NULL DEFAULT '',
                started_at TIMESTAMPTZ NOT NULL,
                finished_at TIMESTAMPTZ NOT NULL,
                attributes_json TEXT NOT NULL DEFAULT '{}',
                created_at TIMESTAMPTZ NOT NULL
            )
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS uk_rag_trace_node_trace_order
                ON t_rag_trace_node (trace_id, node_index)
            """,
            """
            CREATE TABLE IF NOT EXISTS t_rag_trace_tool (
                id BIGSERIAL PRIMARY KEY,
                trace_id VARCHAR(64) NOT NULL,
                tool_index INTEGER NOT NULL,
                tool_id VARCHAR(64) NOT NULL,
                status VARCHAR(64) NOT NULL,
                message TEXT NOT NULL DEFAULT '',
                snapshot_miss BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ NOT NULL
            )
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS uk_rag_trace_tool_trace_order
                ON t_rag_trace_tool (trace_id, tool_index)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_trace_tool_trace
                ON t_rag_trace_tool (trace_id, tool_index)
            """
    );

    /**
     * 执行追踪表结构初始化。
     *
     * @param jdbcTemplate JDBC 执行模板
     */
    public JdbcRagTraceSchemaInitializer(JdbcTemplate jdbcTemplate) {
        DDL.forEach(jdbcTemplate::execute);
    }
}


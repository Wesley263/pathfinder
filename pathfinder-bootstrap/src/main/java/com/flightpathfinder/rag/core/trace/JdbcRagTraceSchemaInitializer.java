package com.flightpathfinder.rag.core.trace;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 创建持久化 trace 所需的 JDBC 表结构。
 *
 * <p>trace 持久化拆分为 run、node、tool 三类表，
 * 使列表查询保持轻量，同时详情查询仍可分别展示阶段节点与 MCP 工具结果。
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
     * 确保当前数据源具备 trace 相关表与索引。
     *
     * @param jdbcTemplate 用于执行 trace DDL 集合的 JDBC 入口
     */
    public JdbcRagTraceSchemaInitializer(JdbcTemplate jdbcTemplate) {
        DDL.forEach(jdbcTemplate::execute);
    }
}

package com.flightpathfinder.rag.core.memory;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the JDBC schema required by conversation memory.
 *
 * <p>Conversation, message and summary tables are split so raw turns remain fully auditable while summary can
 * evolve as a separate compaction layer.
 */
@Component
public class JdbcConversationMemorySchemaInitializer {

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS t_rag_conversation (
                conversation_id VARCHAR(128) PRIMARY KEY,
                last_request_id VARCHAR(128) NOT NULL DEFAULT '',
                turn_count INTEGER NOT NULL DEFAULT 0,
                message_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_rag_conversation_updated_at ON t_rag_conversation (updated_at DESC)",
            """
            CREATE TABLE IF NOT EXISTS t_rag_conversation_message (
                message_id VARCHAR(64) PRIMARY KEY,
                conversation_id VARCHAR(128) NOT NULL,
                request_id VARCHAR(128) NOT NULL,
                message_index INTEGER NOT NULL,
                role VARCHAR(32) NOT NULL,
                content TEXT NOT NULL,
                rewritten_question TEXT NOT NULL DEFAULT '',
                answer_status VARCHAR(64) NOT NULL DEFAULT '',
                created_at TIMESTAMPTZ NOT NULL
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_conversation_message_conversation_created
                ON t_rag_conversation_message (conversation_id, created_at DESC, message_index DESC)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_conversation_message_conversation_request
                ON t_rag_conversation_message (conversation_id, request_id)
            """,
            """
            CREATE TABLE IF NOT EXISTS t_rag_conversation_summary (
                conversation_id VARCHAR(128) PRIMARY KEY,
                summary_text TEXT NOT NULL,
                summarized_turn_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_rag_conversation_summary_updated_at
                ON t_rag_conversation_summary (updated_at DESC)
            """
    );

    /**
     * Ensures the memory tables and indexes exist for the current datasource.
     *
     * @param jdbcTemplate JDBC entry point used to execute the memory DDL set
     */
    public JdbcConversationMemorySchemaInitializer(JdbcTemplate jdbcTemplate) {
        DDL.forEach(jdbcTemplate::execute);
    }
}

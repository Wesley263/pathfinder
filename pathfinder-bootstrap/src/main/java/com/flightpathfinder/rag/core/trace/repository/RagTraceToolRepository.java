package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * Repository for persisted MCP tool summaries inside a trace.
 *
 * <p>Tool summaries are stored separately from stage nodes because operators often want a concise MCP view
 * without re-parsing retrieval node attributes.
 */
public interface RagTraceToolRepository {

    /**
     * Replaces all tool summary rows for the given trace id.
     *
     * @param traceId unique trace identifier
     * @param records tool summary records for the trace
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceToolRecord> records);

    /**
     * Loads all tool summary rows for one trace.
     *
     * @param traceId unique trace identifier
     * @return persisted tool summary rows ordered by tool index
     */
    List<PersistedRagTraceToolRecord> findByTraceId(String traceId);
}

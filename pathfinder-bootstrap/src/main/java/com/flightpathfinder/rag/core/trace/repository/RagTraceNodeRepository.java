package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * Repository for persisted trace nodes.
 *
 * <p>Nodes are stored separately from run headers so stage-level and internal execution breadcrumbs can be
 * queried without bloating the run table.
 */
public interface RagTraceNodeRepository {

    /**
     * Replaces all persisted nodes for the given trace id.
     *
     * @param traceId unique trace identifier
     * @param records fully materialized node records for the trace
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceNodeRecord> records);

    /**
     * Loads all persisted nodes for one trace.
     *
     * @param traceId unique trace identifier
     * @return persisted node records ordered by node index
     */
    List<PersistedRagTraceNodeRecord> findByTraceId(String traceId);
}

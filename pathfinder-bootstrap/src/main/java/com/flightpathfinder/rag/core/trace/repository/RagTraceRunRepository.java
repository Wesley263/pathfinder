package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for persisted trace-run headers.
 *
 * <p>Run records capture one request-level trace summary keyed by trace id and indexed by request id and
 * conversation id.
 */
public interface RagTraceRunRepository {

    /**
     * Inserts or updates one trace-run header.
     *
     * @param record persisted run record for one request
     */
    void upsert(PersistedRagTraceRunRecord record);

    /**
     * Finds one run record by trace id.
     *
     * @param traceId unique trace identifier
     * @return persisted run record when found
     */
    Optional<PersistedRagTraceRunRecord> findByTraceId(String traceId);

    /**
     * Lists recent runs filtered by request or conversation identifiers.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of runs to return
     * @return recent run records
     */
    List<PersistedRagTraceRunRecord> findRecent(String requestId, String conversationId, int limit);
}

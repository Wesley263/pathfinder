package com.flightpathfinder.rag.core.trace;

/**
 * Persistence-facing write service for finalized traces.
 *
 * <p>Write concerns are split from query concerns so the request path can record traces without also owning
 * admin-style read models or list queries.
 */
public interface RagTraceRecordService {

    /**
     * Persists the finalized trace result.
     *
     * @param traceResult completed trace result for one request
     */
    void persist(RagTraceResult traceResult);
}

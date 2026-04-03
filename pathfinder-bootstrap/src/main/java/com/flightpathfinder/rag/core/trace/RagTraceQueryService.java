package com.flightpathfinder.rag.core.trace;

import java.util.List;
import java.util.Optional;

/**
 * Read-facing query service for persisted traces.
 *
 * <p>Query is separated from record so admin and audit views can evolve independently from the request write
 * path. The request path only needs to persist facts; query concerns belong here.
 */
public interface RagTraceQueryService {

    /**
     * Loads one persisted trace detail view.
     *
     * @param traceId unique trace identifier generated for one request
     * @return detailed trace view when found
     */
    Optional<RagTraceDetailResult> findDetail(String traceId);

    /**
     * Lists recent trace runs filtered by request id or conversation id.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of trace runs to return
     * @return recent trace run summaries
     */
    List<RagTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

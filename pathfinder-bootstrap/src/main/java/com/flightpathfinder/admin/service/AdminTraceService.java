package com.flightpathfinder.admin.service;

import java.util.List;
import java.util.Optional;

/**
 * Admin-facing query service for persisted traces.
 *
 * <p>This service exposes trace data in management-oriented shapes so admin APIs do not have to reuse or leak
 * the internal trace query models used by the RAG core.
 */
public interface AdminTraceService {

    /**
     * Finds one admin-facing trace detail view.
     *
     * @param traceId unique trace identifier
     * @return admin trace detail when found
     */
    Optional<AdminTraceDetailResult> findDetail(String traceId);

    /**
     * Lists recent traces filtered by request or conversation identifiers.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of runs to return
     * @return admin-facing trace summaries
     */
    List<AdminTraceRunSummary> listRuns(String requestId, String conversationId, int limit);
}

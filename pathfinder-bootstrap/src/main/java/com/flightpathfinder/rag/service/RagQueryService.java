package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;

/**
 * Synchronous application-facing RAG entry point.
 *
 * <p>This service owns top-level orchestration and is the right place for request-scoped
 * memory and trace lifecycles. Controllers stay thin and do not orchestrate the pipeline
 * directly.</p>
 */
public interface RagQueryService {

    /**
     * Runs the synchronous RAG mainline.
     *
     * @param command query command built from the HTTP request
     * @return end-to-end query result including stage-one, retrieval, answer, and trace data
     */
    RagQueryResult query(RagQueryCommand command);
}

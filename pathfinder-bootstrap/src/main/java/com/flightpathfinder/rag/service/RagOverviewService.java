package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.controller.vo.RagOverviewVO;

/**
 * Read-only overview service for the current RAG wiring.
 *
 * <p>This service exists so structure/overview endpoints can describe the current feature
 * surface without coupling controllers to MCP registry details.</p>
 */
public interface RagOverviewService {

    /**
     * Returns the current overview of the RAG feature wiring.
     *
     * @return overview payload for architecture/operations introspection
     */
    RagOverviewVO currentOverview();
}

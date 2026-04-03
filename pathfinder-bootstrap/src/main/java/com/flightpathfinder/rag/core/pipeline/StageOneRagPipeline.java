package com.flightpathfinder.rag.core.pipeline;

/**
 * First-stage RAG pipeline boundary.
 *
 * <p>This stage is intentionally limited to rewrite, intent classification, and split
 * preparation. Retrieval and answer generation stay in later stages so the orchestration
 * boundary remains explicit.</p>
 */
public interface StageOneRagPipeline {

    /**
     * Runs stage one for a user request.
     *
     * @param request original question plus request-scoped context such as memory
     * @return rewritten question, resolved intents, and the KB/MCP split hand-off
     */
    StageOneRagResult run(StageOneRagRequest request);
}

package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;

/**
 * Retrieval-stage orchestration boundary.
 *
 * <p>This stage receives the already-split stage-one result and invokes KB retrieval and MCP
 * execution independently before they are rejoined for answer generation.</p>
 */
public interface RetrievalService {

    /**
     * Runs the retrieval stage for a completed stage-one result.
     *
     * @param stageOneRagResult rewrite and split output from stage one
     * @return combined retrieval result containing KB and MCP contexts
     */
    RetrievalResult retrieve(StageOneRagResult stageOneRagResult);
}

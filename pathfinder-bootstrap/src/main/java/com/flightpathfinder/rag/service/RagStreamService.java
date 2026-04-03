package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;

/**
 * Streaming application-facing RAG entry point.
 *
 * <p>This service reuses the same mainline stages as the synchronous query service, but emits
 * stage and answer progress as SSE events instead of returning one blocking response.</p>
 */
public interface RagStreamService {

    /**
     * Runs the streaming RAG mainline and writes SSE events to the provided writer.
     *
     * @param command stream command built from the HTTP request
     * @param eventWriter sink for stage, chunk, and completion events
     */
    void stream(RagStreamCommand command, RagStreamEventWriter eventWriter);
}

package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.Map;

/**
 * Low-level bridge to framework trace primitives.
 *
 * <p>This contract hides direct {@link com.flightpathfinder.framework.trace.TraceContext} manipulation from
 * the higher-level trace service so recording and persistence policies can evolve independently.
 */
public interface RagTraceRecorder {

    /**
     * Starts a new trace root for the given scene.
     *
     * @param scene trace scene name, for example the RAG chat mainline
     * @return new trace root bound to the current request scope
     */
    TraceRoot startRoot(String scene);

    /**
     * Appends one trace node to the active trace root.
     *
     * @param root active trace root
     * @param name node name, typically a stage or internal sub-step
     * @param startedAt node start time
     * @param finishedAt node finish time
     * @param attributes structured node attributes
     * @return appended node
     */
    TraceNode appendNode(TraceRoot root, String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes);

    /**
     * Clears the current request-scoped trace context.
     */
    void clear();
}

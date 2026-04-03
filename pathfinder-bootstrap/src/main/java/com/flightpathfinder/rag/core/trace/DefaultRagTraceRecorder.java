package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceContext;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Default bridge from the RAG trace layer to framework trace primitives.
 *
 * <p>This implementation keeps the integration with {@link TraceContext} narrow so the higher-level trace
 * lifecycle can stay focused on request semantics rather than low-level context mutation.
 */
@Service
public class DefaultRagTraceRecorder implements RagTraceRecorder {

    /**
     * Starts a framework trace root for the given scene.
     *
     * @param scene trace scene name
     * @return started trace root
     */
    @Override
    public TraceRoot startRoot(String scene) {
        return TraceContext.startRoot(scene);
    }

    /**
     * Appends one trace node to the provided root.
     *
     * @param root active trace root
     * @param name node name
     * @param startedAt node start time
     * @param finishedAt node finish time
     * @param attributes structured node attributes
     * @return appended node
     */
    @Override
    public TraceNode appendNode(TraceRoot root,
                                String name,
                                Instant startedAt,
                                Instant finishedAt,
                                Map<String, Object> attributes) {
        TraceNode node = new TraceNode(
                name == null ? "" : name.trim(),
                startedAt == null ? Instant.now() : startedAt,
                finishedAt == null ? Instant.now() : finishedAt,
                Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes)));
        root.nodes().add(node);
        return node;
    }

    /**
     * Clears the underlying framework trace context.
     */
    @Override
    public void clear() {
        TraceContext.clear();
    }
}

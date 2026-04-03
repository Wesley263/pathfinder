package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable in-flight trace session for one query.
 *
 * <p>{@code requestId} identifies the external request, {@code conversationId} links the query back to
 * conversation memory when present, and {@code root.traceId()} is the unique trace key persisted for audit
 * lookup.
 */
public final class RagTraceSession {

    private final String requestId;
    private final String conversationId;
    private final TraceRoot root;
    private final List<RagTraceStageResult> stageResults = new ArrayList<>();
    private final List<RagTraceToolSummary> mcpToolSummaries = new ArrayList<>();

    /**
     * Creates a new in-flight trace session.
     *
     * @param requestId external request identifier
     * @param conversationId optional conversation identifier
     * @param root framework trace root backing this session
     */
    public RagTraceSession(String requestId, String conversationId, TraceRoot root) {
        this.requestId = requestId == null ? "" : requestId.trim();
        this.conversationId = conversationId == null ? "" : conversationId.trim();
        this.root = root;
    }

    public String requestId() {
        return requestId;
    }

    public String conversationId() {
        return conversationId;
    }

    public TraceRoot root() {
        return root;
    }

    public List<RagTraceStageResult> stageResults() {
        return stageResults;
    }

    public List<RagTraceToolSummary> mcpToolSummaries() {
        return mcpToolSummaries;
    }
}

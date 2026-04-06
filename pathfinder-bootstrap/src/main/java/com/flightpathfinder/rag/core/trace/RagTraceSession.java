package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.ArrayList;
import java.util.List;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public final class RagTraceSession {

    private final String requestId;
    private final String conversationId;
    private final TraceRoot root;
    private final List<RagTraceStageResult> stageResults = new ArrayList<>();
    private final List<RagTraceToolSummary> mcpToolSummaries = new ArrayList<>();

    /**
     * 说明。
     *
     * @param requestId 外部请求标识
     * @param conversationId 可选会话标识
     * @param root 参数说明。
     */
    public RagTraceSession(String requestId, String conversationId, TraceRoot root) {
        this.requestId = requestId == null ? "" : requestId.trim();
        this.conversationId = conversationId == null ? "" : conversationId.trim();
        this.root = root;
    }

    /**
     * 返回请求标识。
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回会话标识。
     */
    public String conversationId() {
        return conversationId;
    }

    /**
     * 说明。
     */
    public TraceRoot root() {
        return root;
    }

    /**
     * 返回阶段结果集合（可追加）。
     */
    public List<RagTraceStageResult> stageResults() {
        return stageResults;
    }

    /**
     * 说明。
     */
    public List<RagTraceToolSummary> mcpToolSummaries() {
        return mcpToolSummaries;
    }
}

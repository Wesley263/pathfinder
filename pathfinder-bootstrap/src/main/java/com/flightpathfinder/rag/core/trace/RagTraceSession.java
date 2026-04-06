package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.ArrayList;
import java.util.List;

/**
 * Rag 单次请求的追踪会话上下文。
 *
 * 聚合请求标识、追踪根节点、阶段执行结果与工具调用摘要，便于统一输出审计视图。
 */
public final class RagTraceSession {

    private final String requestId;
    private final String conversationId;
    private final TraceRoot root;
    private final List<RagTraceStageResult> stageResults = new ArrayList<>();
    private final List<RagTraceToolSummary> mcpToolSummaries = new ArrayList<>();

    /**
        * 构造追踪会话。
     *
     * @param requestId 外部请求标识
     * @param conversationId 可选会话标识
        * @param root 当前请求对应的追踪根节点
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
     * 返回追踪根节点。
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
     * 返回工具调用摘要集合（可追加）。
     */
    public List<RagTraceToolSummary> mcpToolSummaries() {
        return mcpToolSummaries;
    }
}

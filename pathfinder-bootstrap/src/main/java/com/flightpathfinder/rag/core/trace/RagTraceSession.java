package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.ArrayList;
import java.util.List;

/**
 * 单次查询执行中的可变 trace 会话。
 *
 * <p>{@code requestId} 标识外部请求，{@code conversationId} 在存在时关联会话记忆，
 * {@code root.traceId()} 则是用于持久化审计查询的唯一 trace 键。
 */
public final class RagTraceSession {

    private final String requestId;
    private final String conversationId;
    private final TraceRoot root;
    private final List<RagTraceStageResult> stageResults = new ArrayList<>();
    private final List<RagTraceToolSummary> mcpToolSummaries = new ArrayList<>();

    /**
     * 创建新的执行中 trace 会话。
     *
     * @param requestId 外部请求标识
     * @param conversationId 可选会话标识
     * @param root 支撑该会话的 framework trace 根节点
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
     * 返回底层 trace 根节点。
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
     * 返回 MCP 工具汇总集合（可追加）。
     */
    public List<RagTraceToolSummary> mcpToolSummaries() {
        return mcpToolSummaries;
    }
}

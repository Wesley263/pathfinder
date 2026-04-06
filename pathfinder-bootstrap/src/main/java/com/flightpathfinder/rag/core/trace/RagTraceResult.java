package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceRoot;
import java.util.List;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param traceId 参数说明。
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param scene 场景名称
 * @param overallStatus 整体状态
 * @param snapshotMissOccurred 是否发生快照缺失
 * @param root 根节点
 * @param stages 阶段结果列表
 * @param mcpToolSummaries 参数说明。
 */
public record RagTraceResult(
        String traceId,
        String requestId,
        String conversationId,
        String scene,
        String overallStatus,
        boolean snapshotMissOccurred,
        TraceRoot root,
        List<RagTraceStageResult> stages,
        List<RagTraceToolSummary> mcpToolSummaries) {

    /**
     * 构造时规整文本并完成集合不可变拷贝。
     */
    public RagTraceResult {
        traceId = traceId == null ? "" : traceId.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        scene = scene == null ? "" : scene.trim();
        overallStatus = overallStatus == null || overallStatus.isBlank() ? "UNKNOWN" : overallStatus.trim();
        stages = List.copyOf(stages == null ? List.of() : stages);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }
}



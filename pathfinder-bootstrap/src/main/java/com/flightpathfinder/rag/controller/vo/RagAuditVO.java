package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 面向 RAG 响应的审计视图。
 *
 * @param stageOneCompleted 第一阶段是否完成
 * @param retrievalStatus retrieval 阶段状态
 * @param answerStatus 最终回答状态
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param traceId trace 标识
 * @param traceStatus trace 总状态
 * @param traceStages 阶段级 trace 摘要
 * @param mcpToolSummaries MCP 工具摘要列表
 */
public record RagAuditVO(
        boolean stageOneCompleted,
        String retrievalStatus,
        String answerStatus,
        boolean snapshotMissAffected,
        String requestId,
        String conversationId,
        String traceId,
        String traceStatus,
        List<RagTraceStageVO> traceStages,
        List<RagTraceToolVO> mcpToolSummaries) {
}


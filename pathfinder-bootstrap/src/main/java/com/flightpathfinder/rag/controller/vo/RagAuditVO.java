package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param stageOneCompleted 第一阶段是否完成
 * @param retrievalStatus 参数说明。
 * @param answerStatus 最终回答状态
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param traceId 参数说明。
 * @param traceStatus 参数说明。
 * @param traceStages 参数说明。
 * @param mcpToolSummaries 参数说明。
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




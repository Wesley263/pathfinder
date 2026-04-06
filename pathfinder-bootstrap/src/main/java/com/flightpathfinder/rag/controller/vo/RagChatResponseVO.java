package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 同步聊天接口响应体。
 *
 * @param question 原始问题
 * @param rewrittenQuestion 改写后的问题
 * @param subQuestions 子问题列表
 * @param answerText 最终回答文本
 * @param partial 是否是部分回答
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param audit 审计信息
 * @param kbIntentIds 参数说明。
 * @param mcpIntentIds 参数说明。
 * @param systemIntentIds 参数说明。
 * @param evidenceSummaries 证据摘要列表
 */
public record RagChatResponseVO(
        String question,
        String rewrittenQuestion,
        List<String> subQuestions,
        String answerText,
        boolean partial,
        boolean snapshotMissAffected,
        RagAuditVO audit,
        List<String> kbIntentIds,
        List<String> mcpIntentIds,
        List<String> systemIntentIds,
        List<RagAnswerEvidenceVO> evidenceSummaries) {
}

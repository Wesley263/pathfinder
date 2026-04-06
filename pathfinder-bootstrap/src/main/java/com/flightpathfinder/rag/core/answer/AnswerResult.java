package com.flightpathfinder.rag.core.answer;

import java.util.List;

/**
 * 最终回答结果。
 *
 * 封装回答文本、状态及证据摘要。
 *
 * @param status 回答总体状态
 * @param answerText 最终回答文本
 * @param partial 是否为部分回答
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param empty 是否没有可用回答内容
 * @param evidenceSummaries 回答证据摘要列表
 */
public record AnswerResult(
        String status,
        String answerText,
        boolean partial,
        boolean snapshotMissAffected,
        boolean empty,
        List<AnswerEvidenceSummary> evidenceSummaries) {

    /**
     * 归一化回答结果。
     */
    public AnswerResult {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        answerText = answerText == null ? "" : answerText.trim();
        evidenceSummaries = List.copyOf(evidenceSummaries == null ? List.of() : evidenceSummaries);
        empty = empty || answerText.isBlank();
    }

    /**
     * 创建空回答结果。
     *
     * @param answerText 空回答说明文本
     * @param evidenceSummaries 证据摘要
     * @return 空回答对象
     */
    public static AnswerResult empty(String answerText, List<AnswerEvidenceSummary> evidenceSummaries) {
        return new AnswerResult("EMPTY", answerText, false, false, true, evidenceSummaries);
    }
}

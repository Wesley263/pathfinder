package com.flightpathfinder.rag.core.answer;

/**
 * 回答证据摘要。
 *
 * 说明。
 *
 * @param type 参数说明。
 * @param source 证据来源标识
 * @param label 证据标签或展示名
 * @param status 证据对应状态
 * @param snippet 证据摘要片段
 */
public record AnswerEvidenceSummary(
        String type,
        String source,
        String label,
        String status,
        String snippet) {

    /**
     * 归一化证据摘要字段。
     */
    public AnswerEvidenceSummary {
        type = type == null ? "" : type.trim();
        source = source == null ? "" : source.trim();
        label = label == null ? "" : label.trim();
        status = status == null ? "" : status.trim();
        snippet = snippet == null ? "" : snippet.trim();
    }
}

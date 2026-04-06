package com.flightpathfinder.rag.controller.vo;

/**
 * 回答证据展示对象。
 *
 * @param type 证据类型
 * @param source 证据来源
 * @param label 证据标签
 * @param status 证据状态
 * @param snippet 证据摘要
 */
public record RagAnswerEvidenceVO(
        String type,
        String source,
        String label,
        String status,
        String snippet) {
}

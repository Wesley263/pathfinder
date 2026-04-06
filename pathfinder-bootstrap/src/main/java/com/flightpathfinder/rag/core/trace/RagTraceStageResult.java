package com.flightpathfinder.rag.core.trace;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跟踪阶段结果。
 *
 * @param stageName 阶段名
 * @param status 阶段状态
 * @param summary 阶段摘要
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param attributes 阶段属性
 */
public record RagTraceStageResult(
        String stageName,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {

    /**
     * 构造时规整文本、补齐时间并冻结属性映射。
     */
    public RagTraceStageResult {
        stageName = stageName == null ? "" : stageName.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        summary = summary == null ? "" : summary.trim();
        startedAt = startedAt == null ? Instant.now() : startedAt;
        finishedAt = finishedAt == null ? startedAt : finishedAt;
        attributes = Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
}

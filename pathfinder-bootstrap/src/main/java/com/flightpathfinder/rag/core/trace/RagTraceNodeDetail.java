package com.flightpathfinder.rag.core.trace;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 持久化 trace 节点详情。
 *
 * @param nodeName 节点名称
 * @param nodeType 节点类型（如 STAGE/INTERNAL）
 * @param status 节点状态
 * @param summary 节点摘要
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param attributes 节点属性
 */
public record RagTraceNodeDetail(
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {

    /**
     * 构造时规整文本字段并确保属性映射不可变。
     */
    public RagTraceNodeDetail {
        nodeName = nodeName == null ? "" : nodeName.trim();
        nodeType = nodeType == null || nodeType.isBlank() ? "INTERNAL" : nodeType.trim();
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
        summary = summary == null ? "" : summary.trim();
        attributes = Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
}

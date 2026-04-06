package com.flightpathfinder.rag.controller.vo;

import java.time.Instant;
import java.util.Map;

/**
 * 追踪节点展示对象。
 *
 * @param nodeName 节点名称
 * @param nodeType 节点类型
 * @param status 节点状态
 * @param summary 节点摘要
 * @param startedAt 节点开始时间
 * @param finishedAt 节点结束时间
 * @param attributes 节点附加属性
 */
public record RagTraceNodeVO(
        String nodeName,
        String nodeType,
        String status,
        String summary,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> attributes) {
}


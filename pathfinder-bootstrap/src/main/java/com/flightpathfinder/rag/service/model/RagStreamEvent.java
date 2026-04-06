package com.flightpathfinder.rag.service.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 *
 * @param sequence 事件序号
 * @param event 事件名称
 * @param stage 当前阶段名称
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param traceId 参数说明。
 * @param timestamp 事件时间戳
 * @param status 当前事件状态
 * @param partial 是否是部分结果语义
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param error 错误信息
 * @param data 事件数据体
 */
public record RagStreamEvent(
        long sequence,
        String event,
        String stage,
        String requestId,
        String conversationId,
        String traceId,
        String timestamp,
        String status,
        Boolean partial,
        Boolean snapshotMissAffected,
        String error,
        Map<String, Object> data) {

    /**
     * 归一化流式事件字段。
     */
    public RagStreamEvent {
        event = event == null ? "" : event.trim();
        stage = stage == null ? "" : stage.trim();
        requestId = requestId == null ? "" : requestId.trim();
        conversationId = conversationId == null ? "" : conversationId.trim();
        traceId = traceId == null ? "" : traceId.trim();
        timestamp = timestamp == null ? "" : timestamp.trim();
        status = status == null ? "" : status.trim();
        error = error == null ? "" : error.trim();
        data = Map.copyOf(new LinkedHashMap<>(data == null ? Map.of() : data));
    }
}

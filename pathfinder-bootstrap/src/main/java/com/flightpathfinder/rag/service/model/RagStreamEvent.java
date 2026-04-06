package com.flightpathfinder.rag.service.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流式 RAG 事件模型。
 *
 * <p>它是 SSE 协议层输出的统一事件载体，显式保留 requestId、conversationId、traceId 和阶段状态，
 * 方便前端或调试工具按阶段消费。</p>
 *
 * @param sequence 事件序号
 * @param event 事件名称
 * @param stage 当前阶段名称
 * @param requestId 请求标识
 * @param conversationId 会话标识
 * @param traceId trace 标识
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

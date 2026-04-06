package com.flightpathfinder.rag.controller.vo;

/**
 * 说明。
 *
 * @param toolId 工具标识
 * @param status 工具状态
 * @param message 工具说明信息
 * @param snapshotMiss 是否命中图快照缺失
 */
public record RagTraceToolVO(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss) {
}


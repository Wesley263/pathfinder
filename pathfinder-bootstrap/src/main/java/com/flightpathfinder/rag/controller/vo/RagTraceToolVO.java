package com.flightpathfinder.rag.controller.vo;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
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




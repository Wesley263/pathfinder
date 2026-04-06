package com.flightpathfinder.rag.controller.vo;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param stageName 阶段名称
 * @param status 阶段状态
 * @param summary 阶段摘要
 */
public record RagTraceStageVO(
        String stageName,
        String status,
        String summary) {
}



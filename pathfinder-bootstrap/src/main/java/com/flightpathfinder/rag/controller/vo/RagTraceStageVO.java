package com.flightpathfinder.rag.controller.vo;

/**
 * 说明。
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

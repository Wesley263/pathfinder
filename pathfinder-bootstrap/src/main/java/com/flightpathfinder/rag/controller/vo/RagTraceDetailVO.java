package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 追踪详情展示对象。
 *
 * @param run trace 运行摘要
 * @param stages 阶段级节点列表
 * @param nodes 全量节点列表
 * @param mcpToolSummaries MCP 工具摘要列表
 */
public record RagTraceDetailVO(
        RagTraceRunVO run,
        List<RagTraceNodeVO> stages,
        List<RagTraceNodeVO> nodes,
        List<RagTraceToolVO> mcpToolSummaries) {
}


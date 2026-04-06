package com.flightpathfinder.rag.core.trace;

import java.util.List;

/**
 * 跟踪详情查询聚合结果。
 *
 * @param run 运行级摘要
 * @param stages 阶段级节点列表
 * @param nodes 全量节点列表（含内部节点）
 * @param mcpToolSummaries 参数说明。
 */
public record RagTraceDetailResult(
        RagTraceRunSummary run,
        List<RagTraceNodeDetail> stages,
        List<RagTraceNodeDetail> nodes,
        List<RagTraceToolSummary> mcpToolSummaries) {

    /**
     * 构造时填充默认值并完成集合不可变拷贝。
     */
    public RagTraceDetailResult {
        run = run == null ? new RagTraceRunSummary("", "", "", "", "UNKNOWN", false, null, null, 0, 0) : run;
        stages = List.copyOf(stages == null ? List.of() : stages);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }
}

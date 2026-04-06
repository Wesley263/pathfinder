package com.flightpathfinder.admin.service;

import java.util.List;

/**
 * 管理端服务层数据模型。
 */
public record AdminTraceDetailResult(
        AdminTraceRunSummary run,
        List<AdminTraceNodeSummary> stages,
        List<AdminTraceNodeSummary> nodes,
        List<AdminTraceToolSummary> mcpToolSummaries) {

    public AdminTraceDetailResult {
        run = run == null ? AdminTraceRunSummary.empty() : run;
        stages = List.copyOf(stages == null ? List.of() : stages);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }
}


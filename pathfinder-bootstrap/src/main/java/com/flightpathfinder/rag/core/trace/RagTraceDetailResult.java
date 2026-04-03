package com.flightpathfinder.rag.core.trace;

import java.util.List;

public record RagTraceDetailResult(
        RagTraceRunSummary run,
        List<RagTraceNodeDetail> stages,
        List<RagTraceNodeDetail> nodes,
        List<RagTraceToolSummary> mcpToolSummaries) {

    public RagTraceDetailResult {
        run = run == null ? new RagTraceRunSummary("", "", "", "", "UNKNOWN", false, null, null, 0, 0) : run;
        stages = List.copyOf(stages == null ? List.of() : stages);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        mcpToolSummaries = List.copyOf(mcpToolSummaries == null ? List.of() : mcpToolSummaries);
    }
}

package com.flightpathfinder.rag.controller.vo;

import java.util.List;

public record RagTraceDetailVO(
        RagTraceRunVO run,
        List<RagTraceNodeVO> stages,
        List<RagTraceNodeVO> nodes,
        List<RagTraceToolVO> mcpToolSummaries) {
}

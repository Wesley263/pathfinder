package com.flightpathfinder.admin.controller.vo;

import java.util.List;

public record AdminTraceDetailVO(
        AdminTraceRunVO run,
        List<AdminTraceNodeVO> stages,
        List<AdminTraceNodeVO> nodes,
        List<AdminTraceToolVO> mcpToolSummaries) {
}

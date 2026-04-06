package com.flightpathfinder.admin.controller.vo;

import java.util.List;

/**
 * 管理端响应视图模型。
 */
public record AdminTraceDetailVO(
        AdminTraceRunVO run,
        List<AdminTraceNodeVO> stages,
        List<AdminTraceNodeVO> nodes,
        List<AdminTraceToolVO> mcpToolSummaries) {
}


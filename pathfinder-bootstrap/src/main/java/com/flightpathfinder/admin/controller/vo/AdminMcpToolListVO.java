package com.flightpathfinder.admin.controller.vo;

import java.util.List;

public record AdminMcpToolListVO(
        String status,
        String message,
        int count,
        int availableCount,
        List<AdminMcpToolSummaryVO> tools) {
}

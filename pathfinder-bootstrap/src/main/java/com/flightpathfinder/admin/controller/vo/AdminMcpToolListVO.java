package com.flightpathfinder.admin.controller.vo;

import java.util.List;

/**
 * 管理端响应视图模型。
 */
public record AdminMcpToolListVO(
        String status,
        String message,
        int count,
        int availableCount,
        List<AdminMcpToolSummaryVO> tools) {
}


package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminMcpToolListRequest;
import com.flightpathfinder.admin.controller.vo.AdminMcpToolBodyVO;
import com.flightpathfinder.admin.controller.vo.AdminMcpToolDetailVO;
import com.flightpathfinder.admin.controller.vo.AdminMcpToolListVO;
import com.flightpathfinder.admin.controller.vo.AdminMcpToolSummaryVO;
import com.flightpathfinder.admin.service.AdminMcpService;
import com.flightpathfinder.admin.service.AdminMcpToolDetail;
import com.flightpathfinder.admin.service.AdminMcpToolDetailResult;
import com.flightpathfinder.admin.service.AdminMcpToolListResult;
import com.flightpathfinder.admin.service.AdminMcpToolSummaryItem;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向工具目录巡检的管理端接口。
 *
 * 该控制器用于查看工具描述、可用性与依赖提示，
 * 而不是执行用户侧问答流程，因此归属管理端能力面。
 */
@RestController
@RequestMapping("/api/admin/mcp/tools")
public class AdminMcpController {

    private final AdminMcpService adminMcpService;

    public AdminMcpController(AdminMcpService adminMcpService) {
        this.adminMcpService = adminMcpService;
    }

    /**
     * 基于当前发现状态列出受管工具。
     *
     * @param request 列表请求，控制是否先刷新发现目录
     * @return 面向管理端的工具列表
     */
    @GetMapping
    public Result<AdminMcpToolListVO> list(@ModelAttribute AdminMcpToolListRequest request) {
        AdminMcpToolListResult result = adminMcpService.listTools(request.isRefresh());
        return Results.success(toListVO(result));
    }

    /**
     * 加载单个受管工具详情视图。
     *
     * @param toolId 受管工具标识
     * @param refresh 是否在查询前刷新目录
     * @return 面向管理端的工具详情
     */
    @GetMapping("/{toolId}")
    public Result<AdminMcpToolDetailVO> detail(@PathVariable String toolId,
                                               @RequestParam(defaultValue = "true") boolean refresh) {
        return Results.success(toDetailVO(adminMcpService.findTool(toolId, refresh)));
    }

    private AdminMcpToolListVO toListVO(AdminMcpToolListResult result) {
        return new AdminMcpToolListVO(
                result.status(),
                result.message(),
                result.count(),
                result.availableCount(),
                result.tools().stream()
                        .map(this::toSummaryVO)
                        .toList());
    }

    private AdminMcpToolSummaryVO toSummaryVO(AdminMcpToolSummaryItem item) {
        return new AdminMcpToolSummaryVO(
                item.toolId(),
                item.displayName(),
                item.description(),
                item.source(),
                item.available(),
                item.availabilityMessage(),
                item.dependencySummary(),
                item.dependencyStatus());
    }

    private AdminMcpToolDetailVO toDetailVO(AdminMcpToolDetailResult result) {
        return new AdminMcpToolDetailVO(
                result.toolId(),
                result.status(),
                result.message(),
                result.detail() == null ? null : toBodyVO(result.detail()));
    }

    private AdminMcpToolBodyVO toBodyVO(AdminMcpToolDetail detail) {
        return new AdminMcpToolBodyVO(
                detail.toolId(),
                detail.displayName(),
                detail.description(),
                detail.source(),
                detail.available(),
                detail.availabilityMessage(),
                detail.dependencySummary(),
                detail.dependencyStatus(),
                detail.dependencyDetails(),
                detail.statusHints(),
                detail.inputSchema(),
                detail.outputSchema(),
                detail.remoteBaseUrl());
    }
}

package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rag 概览查询控制器。
 *
 * 对外暴露意图树与路由结构等只读视图，供管理端页面展示。
 */
@RestController
@RequestMapping("/api/rag")
public class RagOverviewController {

    /** Rag 概览查询服务。 */
    private final RagOverviewService ragOverviewService;

    /**
     * 构造概览控制器。
     *
     * @param ragOverviewService 概览查询服务
     */
    public RagOverviewController(RagOverviewService ragOverviewService) {
        this.ragOverviewService = ragOverviewService;
    }

    /**
     * 获取当前 Rag 结构概览。
     *
     * @return 面向结构查看的概览响应
     */
    @GetMapping("/structure")
    public Result<RagOverviewVO> structure() {
        return Results.success(ragOverviewService.currentOverview());
    }
}


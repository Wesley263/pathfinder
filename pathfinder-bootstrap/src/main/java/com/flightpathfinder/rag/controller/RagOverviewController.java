package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向 RAG 结构概览的只读控制器。
 *
 * <p>它只用于运维和结构查看，报告的是当前已实现的 wiring 事实，不参与用户请求主链编排。</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagOverviewController {

    /** 面向 RAG 的概览服务。 */
    private final RagOverviewService ragOverviewService;

    /**
     * 构造概览控制器。
     *
     * @param ragOverviewService RAG 概览服务
     */
    public RagOverviewController(RagOverviewService ragOverviewService) {
        this.ragOverviewService = ragOverviewService;
    }

    /**
     * 返回当前 RAG 结构概览。
     *
     * @return 面向结构查看的概览响应
     */
    @GetMapping("/structure")
    public Result<RagOverviewVO> structure() {
        return Results.success(ragOverviewService.currentOverview());
    }
}


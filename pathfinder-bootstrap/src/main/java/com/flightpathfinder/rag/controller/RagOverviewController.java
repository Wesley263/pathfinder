package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 说明。
 *
 * 说明。
 */
@RestController
@RequestMapping("/api/rag")
public class RagOverviewController {

    /** 注释说明。 */
    private final RagOverviewService ragOverviewService;

    /**
     * 构造概览控制器。
     *
     * @param ragOverviewService 参数说明。
     */
    public RagOverviewController(RagOverviewService ragOverviewService) {
        this.ragOverviewService = ragOverviewService;
    }

    /**
     * 说明。
     *
     * @return 面向结构查看的概览响应
     */
    @GetMapping("/structure")
    public Result<RagOverviewVO> structure() {
        return Results.success(ragOverviewService.currentOverview());
    }
}


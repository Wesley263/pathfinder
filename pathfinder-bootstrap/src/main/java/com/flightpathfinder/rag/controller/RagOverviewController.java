package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only overview controller for the current RAG feature wiring.
 *
 * <p>This endpoint is operational introspection only. It reports implemented structure and
 * does not participate in request orchestration.</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagOverviewController {

    private final RagOverviewService ragOverviewService;

    public RagOverviewController(RagOverviewService ragOverviewService) {
        this.ragOverviewService = ragOverviewService;
    }

    /**
     * Returns the current RAG feature overview.
     *
     * @return structure/overview payload for introspection
     */
    @GetMapping("/structure")
    public Result<RagOverviewVO> structure() {
        return Results.success(ragOverviewService.currentOverview());
    }
}

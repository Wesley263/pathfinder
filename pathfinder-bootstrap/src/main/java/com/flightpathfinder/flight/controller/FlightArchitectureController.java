package com.flightpathfinder.flight.controller;

import com.flightpathfinder.flight.controller.vo.FeatureStructureVO;
import com.flightpathfinder.flight.service.FlightStructureService;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 航班功能结构信息查询控制器。
 *
 * 用于向前端提供当前航班能力的模块与边界结构视图。
 */
@RestController
@RequestMapping("/api/flight")
public class FlightArchitectureController {

    /** 航班结构只读服务。 */
    private final FlightStructureService flightStructureService;

    /**
     * 注入航班结构只读服务。
     *
     * @param flightStructureService 航班结构服务
     */
    public FlightArchitectureController(FlightStructureService flightStructureService) {
        this.flightStructureService = flightStructureService;
    }

    /**
     * 返回当前航班功能结构。
     *
     * @return 功能结构视图
     */
    @GetMapping("/structure")
    public Result<FeatureStructureVO> structure() {
        return Results.success(flightStructureService.currentStructure());
    }
}


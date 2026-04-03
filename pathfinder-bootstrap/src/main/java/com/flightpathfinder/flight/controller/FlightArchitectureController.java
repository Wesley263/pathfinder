package com.flightpathfinder.flight.controller;

import com.flightpathfinder.flight.controller.vo.FeatureStructureVO;
import com.flightpathfinder.flight.service.FlightStructureService;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flight")
public class FlightArchitectureController {

    private final FlightStructureService flightStructureService;

    public FlightArchitectureController(FlightStructureService flightStructureService) {
        this.flightStructureService = flightStructureService;
    }

    @GetMapping("/structure")
    public Result<FeatureStructureVO> structure() {
        return Results.success(flightStructureService.currentStructure());
    }
}


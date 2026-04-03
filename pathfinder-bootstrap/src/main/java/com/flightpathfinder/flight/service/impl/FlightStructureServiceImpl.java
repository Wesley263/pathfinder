package com.flightpathfinder.flight.service.impl;

import com.flightpathfinder.flight.controller.vo.FeatureStructureVO;
import com.flightpathfinder.flight.service.FlightStructureService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FlightStructureServiceImpl implements FlightStructureService {

    @Override
    public FeatureStructureVO currentStructure() {
        return new FeatureStructureVO(
                "flight",
                "pathfinder-bootstrap",
                "scaffolded",
                List.of(
                        "controller -> service -> core -> dao",
                        "graph, pricing, and pruning stay inside the flight feature",
                        "graph snapshots are built, versioned, and published by the main program",
                        "web adapters stay inside flight.controller.vo"
                ));
    }
}

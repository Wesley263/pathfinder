package com.flightpathfinder.flight.controller;

import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotLifecycleService;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.web.Results;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flight/graph-snapshots")
public class FlightGraphSnapshotController {

    private final GraphSnapshotLifecycleService graphSnapshotLifecycleService;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public FlightGraphSnapshotController(GraphSnapshotLifecycleService graphSnapshotLifecycleService,
                                         GraphSnapshotProperties graphSnapshotProperties) {
        this.graphSnapshotLifecycleService = graphSnapshotLifecycleService;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild(@RequestParam(required = false) String graphKey,
                                               @RequestParam(defaultValue = "manual") String reason) {
        GraphSnapshot snapshot = graphSnapshotLifecycleService.rebuild(resolveGraphKey(graphKey), reason);
        return Results.success(Map.of(
                "graphKey", snapshot.graphKey(),
                "snapshotVersion", snapshot.snapshotVersion(),
                "schemaVersion", snapshot.schemaVersion(),
                "nodeCount", snapshot.nodes().size(),
                "edgeCount", snapshot.edges().size()
        ));
    }

    @DeleteMapping
    public Result<Map<String, Object>> invalidate(@RequestParam(required = false) String graphKey) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        graphSnapshotLifecycleService.invalidate(resolvedGraphKey);
        return Results.success(Map.of(
                "graphKey", resolvedGraphKey,
                "status", "INVALIDATED"
        ));
    }

    private String resolveGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey;
    }
}

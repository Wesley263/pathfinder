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

/**
 * 航班图快照管理控制器。
 *
 * 提供图快照重建与失效的运维接口，服务于图读模型发布流程。
 */
@RestController
@RequestMapping("/api/flight/graph-snapshots")
public class FlightGraphSnapshotController {

    /** 图快照生命周期服务。 */
    private final GraphSnapshotLifecycleService graphSnapshotLifecycleService;
    /** 图快照基础配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
     * 注入图快照管理所需依赖。
     *
     * @param graphSnapshotLifecycleService 图快照生命周期服务
     * @param graphSnapshotProperties 图快照配置
     */
    public FlightGraphSnapshotController(GraphSnapshotLifecycleService graphSnapshotLifecycleService,
                                         GraphSnapshotProperties graphSnapshotProperties) {
        this.graphSnapshotLifecycleService = graphSnapshotLifecycleService;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * 重建指定图键的图快照。
     *
     * @param graphKey 可选图键，空值使用默认图
     * @param reason 重建原因
     * @return 重建后的快照摘要信息
     */
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

    /**
     * 失效指定图键的已发布快照。
     *
     * @param graphKey 可选图键，空值使用默认图
     * @return 失效执行结果
     */
    @DeleteMapping
    public Result<Map<String, Object>> invalidate(@RequestParam(required = false) String graphKey) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        graphSnapshotLifecycleService.invalidate(resolvedGraphKey);
        return Results.success(Map.of(
                "graphKey", resolvedGraphKey,
                "status", "INVALIDATED"
        ));
    }

    /**
     * 解析图键，空值回落到默认图。
     *
     * @param graphKey 输入图键
     * @return 实际使用的图键
     */
    private String resolveGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey;
    }
}

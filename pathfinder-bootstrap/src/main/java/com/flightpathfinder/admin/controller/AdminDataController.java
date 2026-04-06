package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminCacheInvalidateRequest;
import com.flightpathfinder.admin.controller.request.AdminDataReloadRequest;
import com.flightpathfinder.admin.controller.vo.AdminCacheInvalidateVO;
import com.flightpathfinder.admin.controller.vo.AdminDataReloadVO;
import com.flightpathfinder.admin.controller.vo.AdminDatasetReloadVO;
import com.flightpathfinder.admin.controller.vo.AdminDataStatsVO;
import com.flightpathfinder.admin.service.AdminCacheInvalidateResult;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import com.flightpathfinder.admin.service.AdminDataReloadResult;
import com.flightpathfinder.admin.service.AdminDataService;
import com.flightpathfinder.admin.service.AdminDataStats;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据导入、统计与缓存维护接口。
 *
 * 该控制器面向运维管理场景，暴露导入数据集与缓存状态的操作能力，
 * 与用户侧接口以及图快照管理接口保持职责隔离。
 */
@RestController
@RequestMapping("/api/admin/data")
public class AdminDataController {

    private final AdminDataService adminDataService;

    public AdminDataController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    /**
     * 触发管理端外部数据集导入。
     *
     * @param request 可选重载请求，包含图键与操作原因
     * @return 重载结果摘要，包含数据集级执行结果与当前统计
     */
    @PostMapping("/reload")
    public Result<AdminDataReloadVO> reload(@RequestBody(required = false) AdminDataReloadRequest request) {
        AdminDataReloadResult result = adminDataService.reload(
                request == null ? null : request.graphKey(),
                request == null ? null : request.reason());
        return Results.success(toReloadVO(result));
    }

    /**
     * 返回管理端数据表当前聚合统计。
     *
     * @return 当前数据统计快照
     */
    @GetMapping("/stats")
    public Result<AdminDataStatsVO> stats() {
        return Results.success(toStatsVO(adminDataService.currentStats()));
    }

    /**
     * 失效依赖导入数据的缓存面。
     *
     * @param request 可选失效请求，包含图键与操作原因
     * @return 缓存失效摘要
     */
    @PostMapping("/cache/invalidate")
    public Result<AdminCacheInvalidateVO> invalidateCaches(@RequestBody(required = false) AdminCacheInvalidateRequest request) {
        AdminCacheInvalidateResult result = adminDataService.invalidateCaches(
                request == null ? null : request.graphKey(),
                request == null ? null : request.reason());
        return Results.success(toCacheInvalidateVO(result));
    }

    private AdminDataReloadVO toReloadVO(AdminDataReloadResult result) {
        return new AdminDataReloadVO(
                result.graphKey(),
                result.overallStatus(),
                result.reason(),
                result.reloadedAt(),
                result.datasetResults().stream().map(this::toDatasetReloadVO).toList(),
                toStatsVO(result.stats()),
                result.cacheInvalidatedScopes(),
                result.graphSnapshotInvalidated());
    }

    private AdminDataStatsVO toStatsVO(AdminDataStats stats) {
        return new AdminDataStatsVO(
                stats.airportCount(),
                stats.airlineCount(),
                stats.routeCount(),
                stats.visaPolicyCount(),
                stats.cityCostCount(),
                stats.queriedAt());
    }

    private AdminCacheInvalidateVO toCacheInvalidateVO(AdminCacheInvalidateResult result) {
        return new AdminCacheInvalidateVO(
                result.graphKey(),
                result.status(),
                result.reason(),
                result.invalidatedAt(),
                result.clearedGraphSnapshotKeyCount(),
                result.clearedMcpToolCount(),
                result.clearedScopes());
    }

    private AdminDatasetReloadVO toDatasetReloadVO(AdminDatasetReloadResult result) {
        return new AdminDatasetReloadVO(
                result.datasetId(),
                result.status(),
                result.reason(),
                result.sourceLocations(),
                result.processedCount(),
                result.upsertedCount(),
                result.failedCount(),
                result.startedAt(),
                result.completedAt(),
                result.details());
    }
}

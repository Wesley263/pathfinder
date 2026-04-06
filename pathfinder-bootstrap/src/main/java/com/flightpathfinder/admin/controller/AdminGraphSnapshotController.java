package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminGraphSnapshotRebuildRequest;
import com.flightpathfinder.admin.controller.vo.AdminGraphSnapshotVO;
import com.flightpathfinder.admin.service.AdminGraphSnapshotService;
import com.flightpathfinder.admin.service.AdminGraphSnapshotStatus;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图快照生命周期管理的管理端接口。
 *
 * 图快照查询、重建与失效属于独立运维关注点，
 * 因此在管理端命名空间下单独暴露，不与通用数据重载混合。
 */
@RestController
@RequestMapping("/api/admin/graph/snapshots")
public class AdminGraphSnapshotController {

    private final AdminGraphSnapshotService adminGraphSnapshotService;

    public AdminGraphSnapshotController(AdminGraphSnapshotService adminGraphSnapshotService) {
        this.adminGraphSnapshotService = adminGraphSnapshotService;
    }

    /**
     * 返回当前已发布图快照状态。
     *
     * @param graphKey 可选图键，空值表示默认图
     * @return 面向管理端的快照状态
     */
    @GetMapping("/current")
    public Result<AdminGraphSnapshotVO> current(@RequestParam(required = false) String graphKey) {
        return Results.success(toVO(adminGraphSnapshotService.currentSnapshot(graphKey)));
    }

    /**
     * 为指定图键重建图快照。
     *
     * @param request 可选重建请求，包含图键与操作原因
     * @return 重建后的快照状态
     */
    @PostMapping("/rebuild")
    public Result<AdminGraphSnapshotVO> rebuild(@RequestBody(required = false) AdminGraphSnapshotRebuildRequest request) {
        return Results.success(toVO(adminGraphSnapshotService.rebuild(
                request == null ? null : request.graphKey(),
                request == null ? null : request.reason())));
    }

    /**
     * 仅失效当前图快照，不执行重建。
     *
     * @param graphKey 可选图键，空值表示默认图
     * @param reason 可选操作原因
     * @return 失效执行状态
     */
    @DeleteMapping
    public Result<AdminGraphSnapshotVO> invalidate(@RequestParam(required = false) String graphKey,
                                                   @RequestParam(required = false) String reason) {
        return Results.success(toVO(adminGraphSnapshotService.invalidate(graphKey, reason)));
    }

    private AdminGraphSnapshotVO toVO(AdminGraphSnapshotStatus status) {
        return new AdminGraphSnapshotVO(
                status.graphKey(),
                status.status(),
                status.snapshotVersion(),
                status.schemaVersion(),
                status.generatedAt(),
                status.observedAt(),
                status.nodeCount(),
                status.edgeCount(),
                status.sourceFingerprint(),
                status.reason(),
                status.metadata());
    }
}

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
 * Admin API for graph snapshot lifecycle management.
 *
 * <p>Graph snapshot operations live under a dedicated admin namespace because snapshot inspection, rebuild and
 * invalidation are distinct operational concerns from generic data reload.
 */
@RestController
@RequestMapping("/api/admin/graph/snapshots")
public class AdminGraphSnapshotController {

    private final AdminGraphSnapshotService adminGraphSnapshotService;

    public AdminGraphSnapshotController(AdminGraphSnapshotService adminGraphSnapshotService) {
        this.adminGraphSnapshotService = adminGraphSnapshotService;
    }

    /**
     * Returns the current published graph snapshot status.
     *
     * @param graphKey optional graph key; blank means the default graph
     * @return admin-facing snapshot status
     */
    @GetMapping("/current")
    public Result<AdminGraphSnapshotVO> current(@RequestParam(required = false) String graphKey) {
        return Results.success(toVO(adminGraphSnapshotService.currentSnapshot(graphKey)));
    }

    /**
     * Rebuilds the graph snapshot for the requested graph key.
     *
     * @param request optional rebuild request with graph key and operator reason
     * @return rebuilt snapshot status
     */
    @PostMapping("/rebuild")
    public Result<AdminGraphSnapshotVO> rebuild(@RequestBody(required = false) AdminGraphSnapshotRebuildRequest request) {
        return Results.success(toVO(adminGraphSnapshotService.rebuild(
                request == null ? null : request.graphKey(),
                request == null ? null : request.reason())));
    }

    /**
     * Invalidates the current graph snapshot without rebuilding it.
     *
     * @param graphKey optional graph key; blank means the default graph
     * @param reason optional operator reason
     * @return invalidation status
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

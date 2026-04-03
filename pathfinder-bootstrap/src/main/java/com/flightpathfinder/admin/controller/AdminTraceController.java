package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminTraceListRequest;
import com.flightpathfinder.admin.controller.vo.AdminTraceDetailVO;
import com.flightpathfinder.admin.controller.vo.AdminTraceNodeVO;
import com.flightpathfinder.admin.controller.vo.AdminTraceRunVO;
import com.flightpathfinder.admin.controller.vo.AdminTraceToolVO;
import com.flightpathfinder.admin.service.AdminTraceDetailResult;
import com.flightpathfinder.admin.service.AdminTraceNodeSummary;
import com.flightpathfinder.admin.service.AdminTraceRunSummary;
import com.flightpathfinder.admin.service.AdminTraceService;
import com.flightpathfinder.admin.service.AdminTraceToolSummary;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.web.Results;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for persisted trace inspection.
 *
 * <p>This controller exposes management-oriented trace views and keeps them separate from any user-facing
 * response models or request execution endpoints.
 */
@RestController
@RequestMapping("/api/admin/traces")
public class AdminTraceController {

    private final AdminTraceService adminTraceService;

    public AdminTraceController(AdminTraceService adminTraceService) {
        this.adminTraceService = adminTraceService;
    }

    /**
     * Loads one trace detail view for admin inspection.
     *
     * @param traceId unique trace id
     * @return admin-facing trace detail
     */
    @GetMapping("/{traceId}")
    public Result<AdminTraceDetailVO> detail(@PathVariable String traceId) {
        AdminTraceDetailResult detailResult = adminTraceService.findDetail(traceId)
                .orElseThrow(() -> new ServiceException(BaseErrorCode.CLIENT_ERROR, "trace not found: " + traceId));
        return Results.success(toDetailVO(detailResult));
    }

    /**
     * Lists recent traces filtered by request or conversation identifiers.
     *
     * @param request list request carrying optional filters and a limit
     * @return admin-facing trace run list
     */
    @GetMapping
    public Result<List<AdminTraceRunVO>> list(@ModelAttribute AdminTraceListRequest request) {
        int normalizedLimit = Math.max(1, Math.min(request.getLimit(), 100));
        return Results.success(adminTraceService.listRuns(request.getRequestId(), request.getConversationId(), normalizedLimit).stream()
                .map(this::toRunVO)
                .toList());
    }

    private AdminTraceDetailVO toDetailVO(AdminTraceDetailResult detailResult) {
        return new AdminTraceDetailVO(
                toRunVO(detailResult.run()),
                detailResult.stages().stream().map(this::toNodeVO).toList(),
                detailResult.nodes().stream().map(this::toNodeVO).toList(),
                detailResult.mcpToolSummaries().stream().map(this::toToolVO).toList());
    }

    private AdminTraceRunVO toRunVO(AdminTraceRunSummary runSummary) {
        return new AdminTraceRunVO(
                runSummary.traceId(),
                runSummary.requestId(),
                runSummary.conversationId(),
                runSummary.scene(),
                runSummary.overallStatus(),
                runSummary.snapshotMissOccurred(),
                runSummary.partial(),
                runSummary.errorOccurred(),
                runSummary.startedAt(),
                runSummary.finishedAt(),
                runSummary.nodeCount(),
                runSummary.toolCount(),
                runSummary.stages().stream().map(this::toNodeVO).toList(),
                runSummary.mcpToolSummaries().stream().map(this::toToolVO).toList());
    }

    private AdminTraceNodeVO toNodeVO(AdminTraceNodeSummary nodeSummary) {
        return new AdminTraceNodeVO(
                nodeSummary.nodeName(),
                nodeSummary.nodeType(),
                nodeSummary.status(),
                nodeSummary.summary(),
                nodeSummary.startedAt(),
                nodeSummary.finishedAt(),
                nodeSummary.partial(),
                nodeSummary.snapshotMiss(),
                nodeSummary.error(),
                nodeSummary.attributes());
    }

    private AdminTraceToolVO toToolVO(AdminTraceToolSummary toolSummary) {
        return new AdminTraceToolVO(
                toolSummary.toolId(),
                toolSummary.status(),
                toolSummary.message(),
                toolSummary.snapshotMiss(),
                toolSummary.error());
    }
}

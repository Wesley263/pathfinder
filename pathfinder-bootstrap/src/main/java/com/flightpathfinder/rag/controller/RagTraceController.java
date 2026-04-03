package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.rag.controller.vo.RagTraceDetailVO;
import com.flightpathfinder.rag.controller.vo.RagTraceNodeVO;
import com.flightpathfinder.rag.controller.vo.RagTraceRunVO;
import com.flightpathfinder.rag.controller.vo.RagTraceToolVO;
import com.flightpathfinder.rag.core.trace.RagTraceDetailResult;
import com.flightpathfinder.rag.core.trace.RagTraceNodeDetail;
import com.flightpathfinder.rag.core.trace.RagTraceQueryService;
import com.flightpathfinder.rag.core.trace.RagTraceRunSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only trace controller for already-recorded RAG executions.
 *
 * <p>This controller exposes persisted/queryable trace data and does not own trace recording.
 * Recording stays in the orchestration layer so query APIs cannot accidentally become part of
 * the live request lifecycle.</p>
 */
@RestController
@RequestMapping("/api/rag/traces")
public class RagTraceController {

    private final RagTraceQueryService ragTraceQueryService;

    public RagTraceController(RagTraceQueryService ragTraceQueryService) {
        this.ragTraceQueryService = ragTraceQueryService;
    }

    /**
     * Returns one trace detail view by trace id.
     *
     * @param traceId persisted trace id
     * @return detailed trace payload for audit and troubleshooting
     */
    @GetMapping("/{traceId}")
    public Result<RagTraceDetailVO> detail(@PathVariable String traceId) {
        RagTraceDetailResult detailResult = ragTraceQueryService.findDetail(traceId)
                .orElseThrow(() -> new ServiceException(BaseErrorCode.CLIENT_ERROR, "trace not found: " + traceId));
        return Results.success(toDetail(detailResult));
    }

    /**
     * Lists recent traces for a request id or conversation id filter.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit max number of rows to return
     * @return recent trace runs matching the supplied filters
     */
    @GetMapping
    public Result<List<RagTraceRunVO>> list(@RequestParam(required = false) String requestId,
                                            @RequestParam(required = false) String conversationId,
                                            @RequestParam(defaultValue = "20") int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        return Results.success(ragTraceQueryService.listRuns(requestId, conversationId, normalizedLimit).stream()
                .map(this::toRun)
                .toList());
    }

    private RagTraceDetailVO toDetail(RagTraceDetailResult detailResult) {
        return new RagTraceDetailVO(
                toRun(detailResult.run()),
                detailResult.stages().stream().map(this::toNode).toList(),
                detailResult.nodes().stream().map(this::toNode).toList(),
                detailResult.mcpToolSummaries().stream()
                        .map(tool -> new RagTraceToolVO(
                                tool.toolId(),
                                tool.status(),
                                tool.message(),
                                tool.snapshotMiss()))
                        .toList());
    }

    private RagTraceRunVO toRun(RagTraceRunSummary runSummary) {
        return new RagTraceRunVO(
                runSummary.traceId(),
                runSummary.requestId(),
                runSummary.conversationId(),
                runSummary.scene(),
                runSummary.overallStatus(),
                runSummary.snapshotMissOccurred(),
                runSummary.startedAt(),
                runSummary.finishedAt(),
                runSummary.nodeCount(),
                runSummary.toolCount());
    }

    private RagTraceNodeVO toNode(RagTraceNodeDetail nodeDetail) {
        return new RagTraceNodeVO(
                nodeDetail.nodeName(),
                nodeDetail.nodeType(),
                nodeDetail.status(),
                nodeDetail.summary(),
                nodeDetail.startedAt(),
                nodeDetail.finishedAt(),
                nodeDetail.attributes());
    }
}

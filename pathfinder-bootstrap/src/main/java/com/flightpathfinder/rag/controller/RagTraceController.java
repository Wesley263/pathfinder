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
 * RAG trace 查询接口。
 *
 * 提供 trace 详情查询和运行记录列表查询能力。
 */
@RestController
@RequestMapping("/api/rag/traces")
public class RagTraceController {

    /** trace 查询服务。 */
    private final RagTraceQueryService ragTraceQueryService;

    /**
     * 构造 trace 控制器。
     *
     * @param ragTraceQueryService trace 查询服务
     */
    public RagTraceController(RagTraceQueryService ragTraceQueryService) {
        this.ragTraceQueryService = ragTraceQueryService;
    }

    /**
        * 查询指定 traceId 的完整详情。
     *
        * @param traceId trace 标识
        * @return trace 详情视图
     */
    @GetMapping("/{traceId}")
    public Result<RagTraceDetailVO> detail(@PathVariable String traceId) {
        RagTraceDetailResult detailResult = ragTraceQueryService.findDetail(traceId)
                .orElseThrow(() -> new ServiceException(BaseErrorCode.CLIENT_ERROR, "trace not found: " + traceId));
        return Results.success(toDetail(detailResult));
    }

    /**
        * 按条件分页查询 trace 运行摘要。
     *
     * @param requestId 可选请求标识过滤条件
     * @param conversationId 可选会话标识过滤条件
     * @param limit 返回条数上限
        * @return trace 运行摘要列表
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

    /**
        * 转换 trace 详情领域对象为展示对象。
     *
        * @param detailResult 详情领域对象
     * @return 展示对象
     */
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

    /**
        * 转换运行摘要领域对象为展示对象。
     *
        * @param runSummary 运行摘要领域对象
     * @return 展示对象
     */
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

    /**
        * 转换节点详情领域对象为展示对象。
     *
        * @param nodeDetail 节点详情领域对象
     * @return 展示对象
     */
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

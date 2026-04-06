package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminTraceDetailResult;
import com.flightpathfinder.admin.service.AdminTraceNodeSummary;
import com.flightpathfinder.admin.service.AdminTraceRunSummary;
import com.flightpathfinder.admin.service.AdminTraceService;
import com.flightpathfinder.admin.service.AdminTraceToolSummary;
import com.flightpathfinder.rag.core.trace.RagTraceDetailResult;
import com.flightpathfinder.rag.core.trace.RagTraceNodeDetail;
import com.flightpathfinder.rag.core.trace.RagTraceQueryService;
import com.flightpathfinder.rag.core.trace.RagTraceRunSummary;
import com.flightpathfinder.rag.core.trace.RagTraceToolSummary;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 持久化追踪查询的管理端适配默认实现。
 *
 * 负责把追踪领域模型转换为管理端可展示的汇总与详情视图。
 */
@Service
public class DefaultAdminTraceService implements AdminTraceService {

    private final RagTraceQueryService ragTraceQueryService;

    public DefaultAdminTraceService(RagTraceQueryService ragTraceQueryService) {
        this.ragTraceQueryService = ragTraceQueryService;
    }

    /**
        * 加载单条追踪的管理端详情视图。
     *
     * @param traceId 追踪标识
     * @return 追踪详情
     */
    @Override
    public Optional<AdminTraceDetailResult> findDetail(String traceId) {
        return ragTraceQueryService.findDetail(traceId)
                .map(this::toDetailResult);
    }

    /**
        * 列出最近追踪供管理端巡检。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 追踪运行汇总列表
     */
    @Override
    public List<AdminTraceRunSummary> listRuns(String requestId, String conversationId, int limit) {
        return ragTraceQueryService.listRuns(requestId, conversationId, limit).stream()
                .map(this::toRunSummaryWithDetail)
                .toList();
    }

    private AdminTraceRunSummary toRunSummaryWithDetail(RagTraceRunSummary runSummary) {
        Optional<RagTraceDetailResult> detailResult = ragTraceQueryService.findDetail(runSummary.traceId());
        if (detailResult.isPresent()) {
            return toRunSummary(detailResult.get());
        }
        return toRunSummary(runSummary, List.of(), List.of());
    }

    private AdminTraceDetailResult toDetailResult(RagTraceDetailResult detailResult) {
        List<AdminTraceNodeSummary> stages = detailResult.stages().stream()
                .map(this::toNodeSummary)
                .toList();
        List<AdminTraceNodeSummary> nodes = detailResult.nodes().stream()
                .map(this::toNodeSummary)
                .toList();
        List<AdminTraceToolSummary> toolSummaries = detailResult.mcpToolSummaries().stream()
                .map(this::toToolSummary)
                .toList();
        return new AdminTraceDetailResult(
                toRunSummary(detailResult.run(), stages, toolSummaries),
                stages,
                nodes,
                toolSummaries);
    }

    private AdminTraceRunSummary toRunSummary(RagTraceDetailResult detailResult) {
        List<AdminTraceNodeSummary> stages = detailResult.stages().stream()
                .map(this::toNodeSummary)
                .toList();
        List<AdminTraceToolSummary> toolSummaries = detailResult.mcpToolSummaries().stream()
                .map(this::toToolSummary)
                .toList();
        return toRunSummary(detailResult.run(), stages, toolSummaries);
    }

    private AdminTraceRunSummary toRunSummary(RagTraceRunSummary runSummary,
                                              List<AdminTraceNodeSummary> stages,
                                              List<AdminTraceToolSummary> toolSummaries) {
        // 汇总节点和工具状态，生成管理端可直接判读的 partial 与 error 标志。
        boolean partial = stages.stream().anyMatch(AdminTraceNodeSummary::partial);
        boolean errorOccurred = isErrorStatus(runSummary.overallStatus())
                || stages.stream().anyMatch(stage -> isErrorStatus(stage.status()) || !stage.error().isBlank())
                || toolSummaries.stream().anyMatch(AdminTraceToolSummary::error);
        return new AdminTraceRunSummary(
                runSummary.traceId(),
                runSummary.requestId(),
                runSummary.conversationId(),
                runSummary.scene(),
                runSummary.overallStatus(),
                runSummary.snapshotMissOccurred(),
                partial,
                errorOccurred,
                runSummary.startedAt(),
                runSummary.finishedAt(),
                runSummary.nodeCount(),
                runSummary.toolCount(),
                stages,
                toolSummaries);
    }

    private AdminTraceNodeSummary toNodeSummary(RagTraceNodeDetail nodeDetail) {
        boolean partial = booleanAttribute(nodeDetail.attributes(), "partial");
        boolean snapshotMiss = booleanAttribute(nodeDetail.attributes(), "snapshotMiss")
                || booleanAttribute(nodeDetail.attributes(), "snapshotMissAffected");
        String error = firstNonBlank(
                stringAttribute(nodeDetail.attributes(), "error"),
                isErrorStatus(nodeDetail.status()) ? nodeDetail.summary() : "");
        return new AdminTraceNodeSummary(
                nodeDetail.nodeName(),
                nodeDetail.nodeType(),
                nodeDetail.status(),
                nodeDetail.summary(),
                nodeDetail.startedAt(),
                nodeDetail.finishedAt(),
                partial,
                snapshotMiss,
                error,
                nodeDetail.attributes());
    }

    private AdminTraceToolSummary toToolSummary(RagTraceToolSummary toolSummary) {
        return new AdminTraceToolSummary(
                toolSummary.toolId(),
                toolSummary.status(),
                toolSummary.message(),
                toolSummary.snapshotMiss(),
                isErrorStatus(toolSummary.status()));
    }

    private boolean booleanAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes == null ? null : attributes.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue.trim());
        }
        return false;
    }

    private String stringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes == null ? null : attributes.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isErrorStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.equals("FAILED")
                || normalized.equals("ERROR")
                || normalized.endsWith("_ERROR")
                || normalized.equals("INVALID_PARAMETER")
                || normalized.equals("MISSING_REQUIRED");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}


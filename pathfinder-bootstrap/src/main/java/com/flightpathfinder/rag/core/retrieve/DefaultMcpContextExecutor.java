package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.mcp.client.McpToolDiscoveryService;
import com.flightpathfinder.rag.core.mcp.client.RemoteMcpToolExecutor;
import com.flightpathfinder.rag.core.mcp.parameter.McpParameterExtractionResult;
import com.flightpathfinder.rag.core.mcp.parameter.McpParameterExtractor;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * MCP 上下文执行器默认实现。
 *
 * 根据意图分流结果选择工具、抽取参数并执行远端调用，
 * 最终聚合为可供回答阶段使用的 McpContext。
 */
@Service
public class DefaultMcpContextExecutor implements McpContextExecutor {

    /** 路径规划意图标识。 */
    private static final String GRAPH_PATH_INTENT_ID = "path_optimize";
    /** 路径规划工具标识。 */
    private static final String GRAPH_PATH_TOOL_ID = "graph.path.search";
    /** 航班搜索意图标识。 */
    private static final String FLIGHT_SEARCH_INTENT_ID = "flight_search";
    /** 航班搜索工具标识。 */
    private static final String FLIGHT_SEARCH_TOOL_ID = "flight.search";
    /** 比价意图标识。 */
    private static final String PRICE_LOOKUP_INTENT_ID = "price_lookup";
    /** 比价工具标识。 */
    private static final String PRICE_LOOKUP_TOOL_ID = "price.lookup";
    /** 签证查询意图标识。 */
    private static final String VISA_CHECK_INTENT_ID = "visa_check";
    /** 签证查询工具标识。 */
    private static final String VISA_CHECK_TOOL_ID = "visa.check";
    /** 城市成本意图标识。 */
    private static final String CITY_COST_INTENT_ID = "city_cost";
    /** 城市成本工具标识。 */
    private static final String CITY_COST_TOOL_ID = "city.cost";
    /** 风险评估意图标识。 */
    private static final String RISK_EVALUATE_INTENT_ID = "risk_evaluate";
    /** 风险评估工具标识。 */
    private static final String RISK_EVALUATE_TOOL_ID = "risk.evaluate";

    /** 本地工具注册表。 */
    private final LocalMcpToolRegistry localMcpToolRegistry;
    /** 远端工具目录刷新服务。 */
    private final McpToolDiscoveryService mcpToolDiscoveryService;
    /** 远端工具执行器。 */
    private final RemoteMcpToolExecutor remoteMcpToolExecutor;
    /** MCP 参数抽取器。 */
    private final McpParameterExtractor mcpParameterExtractor;

    /**
        * 构造 MCP 上下文执行器。
     *
        * @param localMcpToolRegistry 本地工具注册表
     * @param mcpToolDiscoveryService 工具目录刷新服务
     * @param remoteMcpToolExecutor 远端工具执行器
     * @param mcpParameterExtractor 参数抽取器
     */
    public DefaultMcpContextExecutor(LocalMcpToolRegistry localMcpToolRegistry,
                                     McpToolDiscoveryService mcpToolDiscoveryService,
                                     RemoteMcpToolExecutor remoteMcpToolExecutor,
                                     McpParameterExtractor mcpParameterExtractor) {
        this.localMcpToolRegistry = localMcpToolRegistry;
        this.mcpToolDiscoveryService = mcpToolDiscoveryService;
        this.remoteMcpToolExecutor = remoteMcpToolExecutor;
        this.mcpParameterExtractor = mcpParameterExtractor;
    }

    /**
        * 执行 MCP 意图并聚合上下文结果。
     *
     * @param rewriteResult 改写结果，主要用于参数抽取
        * @param intentSplitResult 意图分流结果
        * @return 聚合后的 MCP 上下文
     */
    @Override
    public McpContext execute(RewriteResult rewriteResult, IntentSplitResult intentSplitResult) {
        IntentSplitResult safeIntentSplitResult = intentSplitResult == null ? IntentSplitResult.empty() : intentSplitResult;
        List<ResolvedIntent> mcpIntents = safeIntentSplitResult.mcpIntents();
        if (mcpIntents.isEmpty()) {
            return McpContext.skipped("no MCP intents matched in the current split result");
        }

        List<McpExecutionRecord> executions = new ArrayList<>();
        for (ResolvedIntent mcpIntent : mcpIntents) {
            executions.add(executeIntent(rewriteResult, mcpIntent));
        }

        return new McpContext(
                resolveContextStatus(executions),
                buildSummary(executions),
                mcpIntents,
                executions);
    }

    /**
        * 执行单个意图对应的 MCP 工具调用。
     *
     * @param rewriteResult 改写结果
        * @param resolvedIntent 已解析意图
     * @return 单次工具执行记录
     */
    private McpExecutionRecord executeIntent(RewriteResult rewriteResult, ResolvedIntent resolvedIntent) {
        String toolId = resolveToolId(resolvedIntent);
        if (toolId.isBlank()) {
            return failureRecord(resolvedIntent, "", Map.of(), "UNSUPPORTED_INTENT",
                    "no MCP tool mapping is available for the resolved intent");
        }

        Optional<McpToolDescriptor> toolDescriptor = resolveToolDescriptor(toolId);
        if (toolDescriptor.isEmpty()) {
            return failureRecord(resolvedIntent, toolId, Map.of(), "TOOL_UNAVAILABLE",
                    "MCP tool is not available in the local registry after discovery refresh");
        }

        // 参数抽取阶段失败时不触发远端调用，直接返回失败语义记录。
        McpParameterExtractionResult extractionResult =
                mcpParameterExtractor.extract(rewriteResult, resolvedIntent, toolDescriptor.get());
        if (!extractionResult.ready()) {
            return new McpExecutionRecord(
                    resolvedIntent.intentId(),
                    resolvedIntent.intentName(),
                    resolvedIntent.question(),
                    toolId,
                    "REMOTE",
                    extractionResult.parameters(),
                    false,
                    extractionResult.status(),
                    "",
                    extractionResult.errorMessage(),
                    false,
                    null,
                    "",
                    null);
        }

        McpToolCallRequest callRequest = new McpToolCallRequest(toolId, extractionResult.parameters());
        McpToolCallResult toolResult = remoteMcpToolExecutor.execute(callRequest);
        return toExecutionRecord(resolvedIntent, callRequest.arguments(), toolResult);
    }

    /**
        * 解析工具描述，必要时触发目录刷新后重试。
     *
     * @param toolId 工具标识
     * @return 工具描述；不存在时返回空
     */
    private Optional<McpToolDescriptor> resolveToolDescriptor(String toolId) {
        Optional<McpToolDescriptor> localDescriptor = localMcpToolRegistry.findByToolId(toolId);
        if (localDescriptor.isPresent()) {
            return localDescriptor;
        }

        // 首次未命中时刷新目录，兼容运行时新增工具的场景。
        List<McpToolDescriptor> refreshedTools = mcpToolDiscoveryService.refreshToolCatalog();
        return localMcpToolRegistry.findByToolId(toolId)
                .or(() -> refreshedTools.stream().filter(tool -> toolId.equals(tool.toolId())).findFirst());
    }

    /**
         * 根据意图解析目标工具标识。
     *
         * @param resolvedIntent 已解析意图
         * @return 工具标识；无法映射时返回空字符串
     */
    private String resolveToolId(ResolvedIntent resolvedIntent) {
        if (resolvedIntent == null) {
            return "";
        }
        if (resolvedIntent.mcpToolId() != null && !resolvedIntent.mcpToolId().isBlank()) {
            return resolvedIntent.mcpToolId();
        }
        return switch (resolvedIntent.intentId()) {
            case GRAPH_PATH_INTENT_ID -> GRAPH_PATH_TOOL_ID;
            case FLIGHT_SEARCH_INTENT_ID -> FLIGHT_SEARCH_TOOL_ID;
            case PRICE_LOOKUP_INTENT_ID -> PRICE_LOOKUP_TOOL_ID;
            case VISA_CHECK_INTENT_ID -> VISA_CHECK_TOOL_ID;
            case CITY_COST_INTENT_ID -> CITY_COST_TOOL_ID;
            case RISK_EVALUATE_INTENT_ID -> RISK_EVALUATE_TOOL_ID;
            default -> "";
        };
    }

    /**
        * 统一转换工具返回结果为执行记录。
     *
        * @param resolvedIntent 已解析意图
     * @param requestParameters 请求参数
     * @param toolResult 原始工具返回结果
     * @return 统一执行记录
     */
    private McpExecutionRecord toExecutionRecord(ResolvedIntent resolvedIntent,
                                                 Map<String, Object> requestParameters,
                                                 McpToolCallResult toolResult) {
        Map<String, Object> structuredContent = toolResult == null || toolResult.structuredContent() == null
                ? Map.of()
                : toolResult.structuredContent();
        String status = stringValue(structuredContent.get("status"));
        if (status.isBlank()) {
            status = toolResult != null && toolResult.success() ? "SUCCESS" : "TOOL_ERROR";
        }

        String message = firstNonBlank(
                stringValue(structuredContent.get("message")),
                toolResult == null ? "" : toolResult.content());
        String error = firstNonBlank(
                toolResult == null ? "" : toolResult.errorMessage(),
                status.equals("TOOL_ERROR") ? "MCP tool call failed" : "");
        boolean snapshotMiss = "SNAPSHOT_MISS".equals(status);
        Boolean retryable = structuredContent.get("retryable") instanceof Boolean value ? value : null;
        String suggestedAction = stringValue(structuredContent.get("suggestedAction"));

        return new McpExecutionRecord(
                resolvedIntent.intentId(),
                resolvedIntent.intentName(),
                resolvedIntent.question(),
                toolResult == null ? resolveToolId(resolvedIntent) : toolResult.toolId(),
                "REMOTE",
                requestParameters,
                toolResult != null && toolResult.success(),
                status,
                message,
                error,
                snapshotMiss,
                retryable,
                suggestedAction,
                toolResult);
    }

    /**
     * 构造失败执行记录。
     *
        * @param resolvedIntent 已解析意图
     * @param toolId 工具标识
     * @param requestParameters 请求参数
     * @param status 失败状态
     * @param errorMessage 错误信息
     * @return 失败执行记录
     */
    private McpExecutionRecord failureRecord(ResolvedIntent resolvedIntent,
                                             String toolId,
                                             Map<String, Object> requestParameters,
                                             String status,
                                             String errorMessage) {
        return new McpExecutionRecord(
                resolvedIntent == null ? "" : resolvedIntent.intentId(),
                resolvedIntent == null ? "" : resolvedIntent.intentName(),
                resolvedIntent == null ? "" : resolvedIntent.question(),
                toolId,
                "NONE",
                requestParameters,
                false,
                status,
                "",
                errorMessage,
                false,
                null,
                "",
                null);
    }

    /**
        * 汇总多次工具执行得到 MCP 上下文状态。
     *
     * @param executions 执行记录列表
        * @return 聚合状态
     */
    private String resolveContextStatus(List<McpExecutionRecord> executions) {
        if (executions.isEmpty()) {
            return "SKIPPED";
        }
        boolean hasSnapshotMiss = executions.stream().anyMatch(McpExecutionRecord::snapshotMiss);
        boolean hasSuccess = executions.stream().anyMatch(execution -> isSuccessfulStatus(execution.status()));
        boolean hasPartialSuccess = executions.stream().anyMatch(execution -> "PARTIAL_SUCCESS".equals(execution.status()));
        boolean hasDataNotFound = executions.stream().anyMatch(execution -> "DATA_NOT_FOUND".equals(execution.status()));
        boolean hasNonSnapshotFailure = executions.stream().anyMatch(execution -> !isNonFailureStatus(execution.status()));

        if (hasSnapshotMiss && !hasSuccess && !hasNonSnapshotFailure) {
            return "SNAPSHOT_MISS";
        }
        if (hasDataNotFound && !hasSuccess && !hasPartialSuccess && !hasSnapshotMiss && !hasNonSnapshotFailure) {
            return "DATA_NOT_FOUND";
        }
        if ((hasPartialSuccess || hasDataNotFound) && !hasNonSnapshotFailure && !hasSnapshotMiss) {
            return "PARTIAL_SUCCESS";
        }
        if (hasSnapshotMiss || hasPartialSuccess || (hasNonSnapshotFailure && (hasSuccess || hasDataNotFound))) {
            return "PARTIAL_FAILURE";
        }
        if (hasNonSnapshotFailure) {
            return "FAILED";
        }
        return "SUCCESS";
    }

    /**
     * 判断状态是否属于“成功或可直接用于回答”的一类。
     *
     * @param status 工具状态
        * @return 是否属于成功语义
     */
    private boolean isSuccessfulStatus(String status) {
        return switch (status) {
            case "SUCCESS", "LOW", "MEDIUM", "HIGH" -> true;
            default -> false;
        };
    }

    /**
     * 判断状态是否属于非失败语义。
     *
     * @param status 工具状态
        * @return 是否属于非失败语义
     */
    private boolean isNonFailureStatus(String status) {
        return switch (status) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "SNAPSHOT_MISS", "LOW", "MEDIUM", "HIGH" -> true;
            default -> false;
        };
    }

    /**
        * 生成可读摘要，格式为 toolId:status 串联。
     *
     * @param executions 执行记录列表
     * @return 摘要文本
     */
    private String buildSummary(List<McpExecutionRecord> executions) {
        if (executions.isEmpty()) {
            return "no MCP execution was attempted";
        }
        return executions.stream()
                .map(execution -> execution.toolId() + ":" + execution.status())
                .reduce((left, right) -> left + " | " + right)
                .orElse("no MCP execution was attempted");
    }

    /**
     * 把任意对象安全转成字符串。
     *
     * @param rawValue 原始值
     * @return 去空白后的字符串
     */
    private String stringValue(Object rawValue) {
        return rawValue == null ? "" : String.valueOf(rawValue).trim();
    }

    /**
     * 返回首个非空白字符串。
     *
     * @param values 候选字符串列表
     * @return 第一个非空白字符串；没有则返回空字符串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}


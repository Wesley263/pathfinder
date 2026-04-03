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
 * Default MCP execution bridge for the retrieval stage.
 *
 * <p>This class translates MCP intents into tool calls, but intentionally stops at a
 * structured MCP context so answer composition remains a later concern.</p>
 */
@Service
public class DefaultMcpContextExecutor implements McpContextExecutor {

    private static final String GRAPH_PATH_INTENT_ID = "path_optimize";
    private static final String GRAPH_PATH_TOOL_ID = "graph.path.search";
    private static final String FLIGHT_SEARCH_INTENT_ID = "flight_search";
    private static final String FLIGHT_SEARCH_TOOL_ID = "flight.search";
    private static final String PRICE_LOOKUP_INTENT_ID = "price_lookup";
    private static final String PRICE_LOOKUP_TOOL_ID = "price.lookup";
    private static final String VISA_CHECK_INTENT_ID = "visa_check";
    private static final String VISA_CHECK_TOOL_ID = "visa.check";
    private static final String CITY_COST_INTENT_ID = "city_cost";
    private static final String CITY_COST_TOOL_ID = "city.cost";
    private static final String RISK_EVALUATE_INTENT_ID = "risk_evaluate";
    private static final String RISK_EVALUATE_TOOL_ID = "risk.evaluate";

    private final LocalMcpToolRegistry localMcpToolRegistry;
    private final McpToolDiscoveryService mcpToolDiscoveryService;
    private final RemoteMcpToolExecutor remoteMcpToolExecutor;
    private final McpParameterExtractor mcpParameterExtractor;

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
     * Executes MCP intents by resolving tools, extracting schema-aware parameters, and
     * invoking the remote MCP executor.
     *
     * @param rewriteResult rewritten question used for parameter extraction
     * @param intentSplitResult split result containing MCP intents
     * @return structured MCP context with per-tool execution records
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

        // Parameter extraction stays explicit so MCP requests remain schema-aware instead of
        // regressing into raw text forwarding or hidden tool routing heuristics.
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

    private Optional<McpToolDescriptor> resolveToolDescriptor(String toolId) {
        Optional<McpToolDescriptor> localDescriptor = localMcpToolRegistry.findByToolId(toolId);
        if (localDescriptor.isPresent()) {
            return localDescriptor;
        }

        // Discovery refresh only happens on registry miss so the steady-state path stays cheap
        // while admin/cache invalidation can still force the catalog to repopulate cleanly.
        List<McpToolDescriptor> refreshedTools = mcpToolDiscoveryService.refreshToolCatalog();
        return localMcpToolRegistry.findByToolId(toolId)
                .or(() -> refreshedTools.stream().filter(tool -> toolId.equals(tool.toolId())).findFirst());
    }

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

    private boolean isSuccessfulStatus(String status) {
        return switch (status) {
            case "SUCCESS", "LOW", "MEDIUM", "HIGH" -> true;
            default -> false;
        };
    }

    private boolean isNonFailureStatus(String status) {
        return switch (status) {
            case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "SNAPSHOT_MISS", "LOW", "MEDIUM", "HIGH" -> true;
            default -> false;
        };
    }

    private String buildSummary(List<McpExecutionRecord> executions) {
        if (executions.isEmpty()) {
            return "no MCP execution was attempted";
        }
        return executions.stream()
                .map(execution -> execution.toolId() + ":" + execution.status())
                .reduce((left, right) -> left + " | " + right)
                .orElse("no MCP execution was attempted");
    }

    private String stringValue(Object rawValue) {
        return rawValue == null ? "" : String.valueOf(rawValue).trim();
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

package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminMcpService;
import com.flightpathfinder.admin.service.AdminMcpToolDetail;
import com.flightpathfinder.admin.service.AdminMcpToolDetailResult;
import com.flightpathfinder.admin.service.AdminMcpToolListResult;
import com.flightpathfinder.admin.service.AdminMcpToolSummaryItem;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.mcp.client.McpClientProperties;
import com.flightpathfinder.rag.core.mcp.client.McpToolDiscoveryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 说明。
 *
 * 说明。
 * 因此位于管理端能力面而非复用用户侧执行模型。
 */
@Service
public class DefaultAdminMcpService implements AdminMcpService {

    private static final String SOURCE = "REMOTE";

    private final LocalMcpToolRegistry localMcpToolRegistry;
    private final McpToolDiscoveryService mcpToolDiscoveryService;
    private final GraphSnapshotQueryService graphSnapshotQueryService;
    private final GraphSnapshotProperties graphSnapshotProperties;
    private final McpClientProperties mcpClientProperties;

    public DefaultAdminMcpService(LocalMcpToolRegistry localMcpToolRegistry,
                                  McpToolDiscoveryService mcpToolDiscoveryService,
                                  GraphSnapshotQueryService graphSnapshotQueryService,
                                  GraphSnapshotProperties graphSnapshotProperties,
                                  McpClientProperties mcpClientProperties) {
        this.localMcpToolRegistry = localMcpToolRegistry;
        this.mcpToolDiscoveryService = mcpToolDiscoveryService;
        this.graphSnapshotQueryService = graphSnapshotQueryService;
        this.graphSnapshotProperties = graphSnapshotProperties;
        this.mcpClientProperties = mcpClientProperties;
    }

    /**
        * 说明。
     *
     * @param refresh 参数说明。
     * @return 返回结果。
     */
    @Override
    public AdminMcpToolListResult listTools(boolean refresh) {
        Map<String, McpToolDescriptor> descriptors = loadCatalog(refresh);
        List<AdminMcpToolSummaryItem> tools = managedDefinitions().values().stream()
                .map(definition -> toSummary(definition, descriptors.get(definition.toolId())))
                .toList();
        int availableCount = (int) tools.stream().filter(AdminMcpToolSummaryItem::available).count();
        return new AdminMcpToolListResult(
                "SUCCESS",
                "MCP tool catalog loaded; " + availableCount + "/" + tools.size() + " tools are currently available",
                tools.size(),
                availableCount,
                tools);
    }

    /**
        * 说明。
     *
     * @param toolId 参数说明。
     * @param refresh 参数说明。
     * @return 返回结果。
     */
    @Override
    public AdminMcpToolDetailResult findTool(String toolId, boolean refresh) {
        String normalizedToolId = toolId == null ? "" : toolId.trim();
        ManagedMcpToolDefinition definition = managedDefinitions().get(normalizedToolId);
        if (definition == null) {
            return new AdminMcpToolDetailResult(
                    normalizedToolId,
                    "NOT_FOUND",
                    "managed MCP tool is not available for toolId=" + normalizedToolId,
                    null);
        }

        Map<String, McpToolDescriptor> descriptors = loadCatalog(refresh);
        McpToolDescriptor descriptor = descriptors.get(definition.toolId());
        DependencyView dependencyView = dependencyView(definition, descriptor != null);
        return new AdminMcpToolDetailResult(
                definition.toolId(),
                "SUCCESS",
                descriptor != null
                        ? "MCP tool detail loaded from the current catalog"
                        : "MCP tool detail loaded from managed metadata; descriptor is not currently available in the catalog",
                new AdminMcpToolDetail(
                        definition.toolId(),
                        descriptor == null ? definition.displayName() : descriptor.displayName(),
                        descriptor == null ? definition.description() : descriptor.description(),
                        SOURCE,
                        descriptor != null,
                        descriptor != null
                                ? "tool is available in the current local registry after discovery refresh"
                                : "tool is not available in the current local registry after discovery refresh",
                        definition.dependencySummary(),
                        dependencyView.status(),
                        dependencyView.details(),
                        definition.statusHints(),
                        descriptor == null ? Map.of() : descriptor.inputSchema(),
                        descriptor == null ? Map.of() : descriptor.outputSchema(),
                        mcpClientProperties.getBaseUrl()));
    }

    private AdminMcpToolSummaryItem toSummary(ManagedMcpToolDefinition definition, McpToolDescriptor descriptor) {
        DependencyView dependencyView = dependencyView(definition, descriptor != null);
        return new AdminMcpToolSummaryItem(
                definition.toolId(),
                descriptor == null ? definition.displayName() : descriptor.displayName(),
                descriptor == null ? definition.description() : descriptor.description(),
                SOURCE,
                descriptor != null,
                descriptor != null
                        ? "tool is available in the current local registry after discovery refresh"
                        : "tool is not available in the current local registry after discovery refresh",
                definition.dependencySummary(),
                dependencyView.status());
    }

    private Map<String, McpToolDescriptor> loadCatalog(boolean refresh) {
        List<McpToolDescriptor> catalog = refresh
                ? mcpToolDiscoveryService.refreshToolCatalog()
                : localMcpToolRegistry.listTools();
        if (catalog.isEmpty() && refresh) {
                        // 即使刷新未返回结果，目录巡检也应可用，因此回退到本地注册表的最近状态视图。
            catalog = localMcpToolRegistry.listTools();
        }
        return catalog.stream()
                .collect(Collectors.toMap(McpToolDescriptor::toolId, Function.identity(), (left, right) -> right, LinkedHashMap::new));
    }

    private DependencyView dependencyView(ManagedMcpToolDefinition definition, boolean available) {
        List<String> details = new java.util.ArrayList<>();
        details.add("Remote MCP endpoint: " + mcpClientProperties.getBaseUrl());
        details.addAll(definition.dependencyDetails());

        if ("graph.path.search".equals(definition.toolId())) {
            String graphKey = graphSnapshotProperties.getDefaultGraphKey();
            Optional<GraphSnapshot> snapshot = graphSnapshotQueryService.loadCurrent(graphKey);
                        // 说明。
            if (snapshot.isPresent()) {
                GraphSnapshot graphSnapshot = snapshot.get();
                details.add("Current graph snapshot is ready for graphKey=" + graphKey + ", snapshotVersion=" + graphSnapshot.snapshotVersion());
                return new DependencyView(available ? "SNAPSHOT_READY" : "SNAPSHOT_READY_BUT_TOOL_UNAVAILABLE", List.copyOf(details));
            }
            details.add("Current graph snapshot is missing for graphKey=" + graphKey);
            return new DependencyView(available ? "SNAPSHOT_MISS" : "SNAPSHOT_MISS_AND_TOOL_UNAVAILABLE", List.copyOf(details));
        }

        if (available) {
            details.add("Tool is exposed by the currently discovered MCP server catalog.");
            return new DependencyView("REMOTE_DEPENDENCIES_READY", List.copyOf(details));
        }
        details.add("Tool is not currently exposed by the discovered MCP server catalog.");
        return new DependencyView("REMOTE_DEPENDENCY_UNKNOWN", List.copyOf(details));
    }

    private Map<String, ManagedMcpToolDefinition> managedDefinitions() {
        return MANAGED_DEFINITIONS;
    }

    private static final Map<String, ManagedMcpToolDefinition> MANAGED_DEFINITIONS = List.of(
            new ManagedMcpToolDefinition(
                    "graph.path.search",
                    "Graph Path Search",
                    "Searches candidate paths from the current graph snapshot in Redis.",
                    "Remote MCP server + Redis graph snapshot",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires a current graph snapshot in Redis for the target graphKey.",
                            "Snapshot miss is returned as a structured business result, not a transport failure."
                    ),
                    List.of("SUCCESS", "NO_PATH_FOUND", "SNAPSHOT_MISS", "INVALID_REQUEST", "EXECUTION_ERROR")),
            new ManagedMcpToolDefinition(
                    "flight.search",
                    "Flight Search",
                    "Searches direct flight options by airport pair and date window using the MCP server datasource.",
                    "Remote MCP server + JDBC datasource",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires MCP server datasource access to airport, route and airline tables."
                    ),
                    List.of("SUCCESS", "NO_FLIGHTS_FOUND", "INVALID_REQUEST", "EXECUTION_ERROR")),
            new ManagedMcpToolDefinition(
                    "price.lookup",
                    "Price Lookup",
                    "Compares lowest prices across city pairs by reusing server-side flight data.",
                    "Remote MCP server + JDBC datasource",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires MCP server datasource access to route and airline data through the price lookup service."
                    ),
                    List.of("SUCCESS", "PARTIAL_SUCCESS", "NO_PRICE_FOUND", "INVALID_REQUEST", "EXECUTION_ERROR")),
            new ManagedMcpToolDefinition(
                    "visa.check",
                    "Visa Check",
                    "Checks visa-free, transit-free, required or missing-data results from visa policy data.",
                    "Remote MCP server + JDBC datasource",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires MCP server datasource access to visa policy data."
                    ),
                    List.of("SUCCESS", "PARTIAL_SUCCESS", "DATA_NOT_FOUND", "INVALID_REQUEST", "EXECUTION_ERROR", "VISA_FREE", "TRANSIT_FREE", "REQUIRED")),
            new ManagedMcpToolDefinition(
                    "city.cost",
                    "City Cost",
                    "Looks up living cost data for requested cities from the MCP server dataset.",
                    "Remote MCP server + JDBC datasource",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires MCP server datasource access to city cost data."
                    ),
                    List.of("SUCCESS", "PARTIAL_SUCCESS", "DATA_NOT_FOUND", "INVALID_REQUEST", "EXECUTION_ERROR")),
            new ManagedMcpToolDefinition(
                    "risk.evaluate",
                    "Risk Evaluate",
                    "Evaluates transfer risk from hub, airline and buffer inputs using the MCP server datasource and rules.",
                    "Remote MCP server + JDBC datasource",
                    List.of(
                            "Requires the MCP server process to be reachable.",
                            "Requires MCP server datasource access to airport and airline reference data."
                    ),
                    List.of("LOW", "MEDIUM", "HIGH", "DATA_NOT_FOUND", "INVALID_REQUEST", "EXECUTION_ERROR"))
    ).stream().collect(Collectors.toMap(ManagedMcpToolDefinition::toolId, Function.identity(), (left, right) -> right, LinkedHashMap::new));

    private record ManagedMcpToolDefinition(
            String toolId,
            String displayName,
            String description,
            String dependencySummary,
            List<String> dependencyDetails,
            List<String> statusHints) {
    }

    private record DependencyView(String status, List<String> details) {
    }
}

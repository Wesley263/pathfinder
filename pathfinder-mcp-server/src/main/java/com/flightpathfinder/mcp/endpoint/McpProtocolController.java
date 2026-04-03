package com.flightpathfinder.mcp.endpoint;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.protocol.mcp.JsonRpcRequest;
import com.flightpathfinder.framework.protocol.mcp.JsonRpcResponse;
import com.flightpathfinder.framework.web.Results;
import com.flightpathfinder.mcp.server.dispatcher.McpDispatcher;
import com.flightpathfinder.mcp.server.registry.McpServerToolRegistry;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for the server-side MCP protocol bridge.
 *
 * <p>This controller only accepts protocol-level requests and delegates routing to the dispatcher. It
 * intentionally stays thin so JSON-RPC transport concerns do not leak into individual tool
 * implementations.
 */
@RestController
@RequestMapping("/mcp")
public class McpProtocolController {

    private final McpDispatcher mcpDispatcher;
    private final McpServerToolRegistry mcpServerToolRegistry;

    public McpProtocolController(McpDispatcher mcpDispatcher, McpServerToolRegistry mcpServerToolRegistry) {
        this.mcpDispatcher = mcpDispatcher;
        this.mcpServerToolRegistry = mcpServerToolRegistry;
    }

    /**
     * Handles a JSON-RPC MCP request.
     *
     * @param request transport-level MCP request carrying the method name and parameters
     * @return JSON-RPC response produced by the dispatcher
     */
    @PostMapping
    public JsonRpcResponse<?> handle(@RequestBody JsonRpcRequest<Map<String, Object>> request) {
        return mcpDispatcher.dispatch(request);
    }

    /**
     * Exposes a lightweight health view for infrastructure checks.
     *
     * @return service status plus the number of registered tools currently exposed by this server
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Results.success(Map.of("status", "ok", "toolCount", mcpServerToolRegistry.listTools().size()));
    }
}

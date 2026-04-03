package com.flightpathfinder.mcp.server.dispatcher;

import com.flightpathfinder.framework.protocol.mcp.JsonRpcError;
import com.flightpathfinder.framework.protocol.mcp.JsonRpcRequest;
import com.flightpathfinder.framework.protocol.mcp.JsonRpcResponse;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolListResult;
import com.flightpathfinder.mcp.server.registry.McpServerToolRegistry;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Routes protocol-level MCP methods to server-side discovery and execution flows.
 *
 * <p>The dispatcher owns JSON-RPC method branching such as {@code tools/list} and {@code tools/call},
 * while the registry remains responsible only for tool lookup. Keeping these concerns separate avoids
 * turning the registry into a protocol-aware service.
 */
@Component
public class McpDispatcher {

    private static final JsonRpcError METHOD_NOT_FOUND = new JsonRpcError(-32601, "Method not found");
    private static final JsonRpcError INVALID_PARAMS = new JsonRpcError(-32602, "Invalid params");

    private final McpServerToolRegistry mcpServerToolRegistry;

    public McpDispatcher(McpServerToolRegistry mcpServerToolRegistry) {
        this.mcpServerToolRegistry = mcpServerToolRegistry;
    }

    /**
     * Dispatches the incoming JSON-RPC method to the appropriate server-side MCP branch.
     *
     * @param request protocol request containing the MCP method and raw parameters
     * @return JSON-RPC success or error response
     */
    public JsonRpcResponse<?> dispatch(JsonRpcRequest<Map<String, Object>> request) {
        // The current HTTP bridge only exposes discovery and invocation. An explicit initialize handshake
        // can be added later without forcing tool executors to understand protocol-level concerns.
        return switch (request.method()) {
            case "tools/list" -> JsonRpcResponse.success(request.id(), new McpToolListResult(mcpServerToolRegistry.listTools()));
            case "tools/call" -> dispatchToolCall(request);
            default -> JsonRpcResponse.error(request.id(), METHOD_NOT_FOUND);
        };
    }

    private JsonRpcResponse<?> dispatchToolCall(JsonRpcRequest<Map<String, Object>> request) {
        McpToolCallRequest callRequest = toCallRequest(request.params());
        if (callRequest == null) {
            return JsonRpcResponse.error(request.id(), INVALID_PARAMS);
        }
        return mcpServerToolRegistry.findExecutor(callRequest.toolId())
                .<JsonRpcResponse<?>>map(executor -> JsonRpcResponse.success(request.id(), executor.execute(callRequest)))
                .orElseGet(() -> JsonRpcResponse.error(request.id(), new JsonRpcError(-32004, "Tool not found: " + callRequest.toolId())));
    }

    @SuppressWarnings("unchecked")
    private McpToolCallRequest toCallRequest(Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        Object toolId = params.get("toolId");
        Object arguments = params.get("arguments");
        if (!(toolId instanceof String toolIdValue)) {
            return null;
        }
        Map<String, Object> argumentMap = arguments instanceof Map<?, ?> argumentSource
                ? (Map<String, Object>) argumentSource
                : Map.of();
        return new McpToolCallRequest(toolIdValue, argumentMap);
    }
}

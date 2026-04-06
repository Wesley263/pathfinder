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
 * 说明。
 *
 * 说明。
 * 注册器仅负责工具查找。分离这两类职责可避免注册器膨胀为协议感知组件。
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
     * 说明。
     *
     * @param request 参数说明。
     * @return 返回结果。
     */
    public JsonRpcResponse<?> dispatch(JsonRpcRequest<Map<String, Object>> request) {
        // 说明。
        // 说明。
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

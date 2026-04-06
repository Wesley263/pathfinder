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
 * 将协议层 MCP 方法路由到服务端发现与执行流程。
 *
 * <p>分发器负责 {@code tools/list}、{@code tools/call} 等 JSON-RPC 方法分支，
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
     * 将入站 JSON-RPC 方法分发到对应的服务端 MCP 分支。
     *
     * @param request 协议请求，包含 MCP 方法与原始参数
     * @return JSON-RPC 成功或错误响应
     */
    public JsonRpcResponse<?> dispatch(JsonRpcRequest<Map<String, Object>> request) {
        // 当前 HTTP 桥只暴露发现与调用能力。
        // 后续可增加显式 initialize 握手，而无需让工具执行器理解协议细节。
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

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
 * 服务端 MCP 协议桥的 HTTP 入口。
 *
 * <p>该控制器只接收协议层请求，并将路由交给分发器处理。
 * 通过保持轻量职责，避免把 JSON-RPC 传输细节渗透到各个工具实现中。
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
     * 处理一条 JSON-RPC MCP 请求。
     *
     * @param request 传输层 MCP 请求，包含方法名与参数
     * @return 分发器产出的 JSON-RPC 响应
     */
    @PostMapping
    public JsonRpcResponse<?> handle(@RequestBody JsonRpcRequest<Map<String, Object>> request) {
        return mcpDispatcher.dispatch(request);
    }

    /**
     * 暴露轻量健康检查视图，供基础设施探活使用。
     *
     * @return 服务状态与当前服务端已注册工具数量
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Results.success(Map.of("status", "ok", "toolCount", mcpServerToolRegistry.listTools().size()));
    }
}

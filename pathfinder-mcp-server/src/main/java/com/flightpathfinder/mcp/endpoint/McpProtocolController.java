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
 * 说明。
 *
 * 说明。
 * 说明。
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
     * 说明。
     *
     * @param request 参数说明。
     * @return 返回结果。
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

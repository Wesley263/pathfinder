package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 说明。
 *
 * 说明。
 * 描述信息一次性由执行器物化，而执行器本身仍是工具契约与执行规则的唯一所有者。
 */
public class InMemoryMcpServerToolRegistry implements McpServerToolRegistry {

    private final Map<String, McpToolExecutor> executorMap;

    public InMemoryMcpServerToolRegistry(List<McpToolExecutor> toolExecutors) {
        this.executorMap = new LinkedHashMap<>();
        toolExecutors.forEach(executor -> executorMap.put(executor.descriptor().toolId(), executor));
    }

    /**
     * 按注册顺序返回工具描述。
     *
     * @return 当前服务进程对外暴露的工具描述列表
     */
    @Override
    public List<McpToolDescriptor> listTools() {
        return executorMap.values()
                .stream()
                .map(McpToolExecutor::descriptor)
                .toList();
    }

    /**
     * 说明。
     *
     * @param toolId 参数说明。
     * @return 已注册则返回执行器，否则返回空
     */
    @Override
    public Optional<McpToolExecutor> findExecutor(String toolId) {
        return Optional.ofNullable(executorMap.get(toolId));
    }
}

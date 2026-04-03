package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-process registry backed by the Spring-managed executor list.
 *
 * <p>This registry keeps server-side tool exposure deterministic and cheap at runtime: descriptors are
 * materialized from executors once, while the executors themselves remain the single owners of tool
 * contracts and execution rules.
 */
public class InMemoryMcpServerToolRegistry implements McpServerToolRegistry {

    private final Map<String, McpToolExecutor> executorMap;

    public InMemoryMcpServerToolRegistry(List<McpToolExecutor> toolExecutors) {
        this.executorMap = new LinkedHashMap<>();
        toolExecutors.forEach(executor -> executorMap.put(executor.descriptor().toolId(), executor));
    }

    /**
     * Returns tool descriptors in registration order.
     *
     * @return descriptors exposed by the current server process
     */
    @Override
    public List<McpToolDescriptor> listTools() {
        return executorMap.values()
                .stream()
                .map(McpToolExecutor::descriptor)
                .toList();
    }

    /**
     * Looks up the executor that owns the requested tool id.
     *
     * @param toolId MCP tool id to resolve
     * @return executor when registered, otherwise empty
     */
    @Override
    public Optional<McpToolExecutor> findExecutor(String toolId) {
        return Optional.ofNullable(executorMap.get(toolId));
    }
}

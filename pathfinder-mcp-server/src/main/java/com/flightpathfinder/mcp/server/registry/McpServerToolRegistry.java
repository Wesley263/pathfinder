package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.List;
import java.util.Optional;

/**
 * Server-side catalog of MCP tools exposed by this process.
 *
 * <p>The registry is deliberately narrow: it only answers "what tools are available?" and "which executor
 * owns this tool?" Protocol dispatch, dependency health checks and business execution all stay outside this
 * abstraction.
 */
public interface McpServerToolRegistry {

    /**
     * Lists descriptors for all tools currently exposed by the MCP server.
     *
     * @return ordered tool descriptors used by {@code tools/list}
     */
    List<McpToolDescriptor> listTools();

    /**
     * Finds the executor responsible for the given tool id.
     *
     * @param toolId MCP tool id such as {@code graph.path.search}
     * @return matching executor when the tool is registered in this process
     */
    Optional<McpToolExecutor> findExecutor(String toolId);
}

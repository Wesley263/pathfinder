package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;

/**
 * Contract implemented by each server-side MCP tool.
 *
 * <p>Executors stay in {@code pathfinder-mcp-server} because each tool owns its own data access and rule
 * logic on the server side. The bootstrap application only talks to these tools through MCP contracts and
 * never shares the Java implementation.
 */
public interface McpToolExecutor {

    /**
     * Describes the tool contract exposed through MCP discovery.
     *
     * @return descriptor containing tool id, human-readable description and input/output schemas
     */
    McpToolDescriptor descriptor();

    /**
     * Executes a tool call using already-decoded MCP request arguments.
     *
     * @param request tool call request containing the target tool id and structured arguments
     * @return structured tool result, including business-level miss or invalid-request states when needed
     */
    McpToolCallResult execute(McpToolCallRequest request);
}

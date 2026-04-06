package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import org.springframework.stereotype.Service;
/**
 * 核心组件。
 */

@Service
public class DefaultRemoteMcpToolExecutor implements RemoteMcpToolExecutor {

    private final McpClient mcpClient;

    public DefaultRemoteMcpToolExecutor(McpClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        return mcpClient.callTool(request);
    }
}



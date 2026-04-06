package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import org.springframework.stereotype.Service;
/**
 * 用于提供当前领域能力的默认实现。
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





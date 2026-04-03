package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;

public interface RemoteMcpToolExecutor {

    McpToolCallResult execute(McpToolCallRequest request);
}


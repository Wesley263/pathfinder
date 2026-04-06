package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
/**
 * 用于提供当前领域能力的默认实现。
 */
public interface RemoteMcpToolExecutor {

    McpToolCallResult execute(McpToolCallRequest request);
}





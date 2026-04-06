package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
/**
 * 用于提供当前领域能力的默认实现。
 */
public interface McpClient {

    List<McpToolDescriptor> listTools();

    McpToolCallResult callTool(McpToolCallRequest request);
}





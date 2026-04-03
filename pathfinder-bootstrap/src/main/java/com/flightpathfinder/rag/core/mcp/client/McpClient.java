package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;

public interface McpClient {

    List<McpToolDescriptor> listTools();

    McpToolCallResult callTool(McpToolCallRequest request);
}


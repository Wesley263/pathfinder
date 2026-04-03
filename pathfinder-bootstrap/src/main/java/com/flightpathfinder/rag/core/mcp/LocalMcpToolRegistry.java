package com.flightpathfinder.rag.core.mcp;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
import java.util.Optional;

public interface LocalMcpToolRegistry {

    List<McpToolDescriptor> listTools();

    Optional<McpToolDescriptor> findByToolId(String toolId);

    void replaceAll(List<McpToolDescriptor> tools);
}


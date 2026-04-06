package com.flightpathfinder.rag.core.mcp;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
import java.util.Optional;
/**
 * 核心组件。
 */
public interface LocalMcpToolRegistry {

    List<McpToolDescriptor> listTools();

    Optional<McpToolDescriptor> findByToolId(String toolId);

    void replaceAll(List<McpToolDescriptor> tools);
}



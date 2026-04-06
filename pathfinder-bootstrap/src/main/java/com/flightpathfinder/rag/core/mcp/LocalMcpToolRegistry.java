package com.flightpathfinder.rag.core.mcp;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
import java.util.Optional;
/**
 * 用于提供当前领域能力的默认实现。
 */
public interface LocalMcpToolRegistry {

    List<McpToolDescriptor> listTools();

    Optional<McpToolDescriptor> findByToolId(String toolId);

    void replaceAll(List<McpToolDescriptor> tools);
}





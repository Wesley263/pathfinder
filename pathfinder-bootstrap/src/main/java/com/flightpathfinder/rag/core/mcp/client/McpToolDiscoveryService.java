package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
/**
 * 核心组件。
 */
public interface McpToolDiscoveryService {

    List<McpToolDescriptor> refreshToolCatalog();
}



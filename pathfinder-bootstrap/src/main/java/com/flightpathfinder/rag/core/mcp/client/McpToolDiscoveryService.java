package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;

public interface McpToolDiscoveryService {

    List<McpToolDescriptor> refreshToolCatalog();
}


package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultMcpToolDiscoveryService implements McpToolDiscoveryService {

    private final McpClient mcpClient;
    private final LocalMcpToolRegistry localMcpToolRegistry;

    public DefaultMcpToolDiscoveryService(McpClient mcpClient, LocalMcpToolRegistry localMcpToolRegistry) {
        this.mcpClient = mcpClient;
        this.localMcpToolRegistry = localMcpToolRegistry;
    }

    @Override
    public List<McpToolDescriptor> refreshToolCatalog() {
        List<McpToolDescriptor> tools = mcpClient.listTools();
        localMcpToolRegistry.replaceAll(tools);
        return tools;
    }
}


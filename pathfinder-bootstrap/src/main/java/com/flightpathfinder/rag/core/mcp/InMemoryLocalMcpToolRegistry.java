package com.flightpathfinder.rag.core.mcp;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
/**
 * 核心组件。
 */

@Component
public class InMemoryLocalMcpToolRegistry implements LocalMcpToolRegistry {

    private final AtomicReference<List<McpToolDescriptor>> toolsHolder = new AtomicReference<>(List.of());

    @Override
    public List<McpToolDescriptor> listTools() {
        return toolsHolder.get();
    }

    @Override
    public Optional<McpToolDescriptor> findByToolId(String toolId) {
        return listTools().stream()
                .filter(tool -> tool.toolId().equals(toolId))
                .findFirst();
    }

    @Override
    public void replaceAll(List<McpToolDescriptor> tools) {
        toolsHolder.set(List.copyOf(tools));
    }
}



package com.flightpathfinder.mcp.config;

import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import com.flightpathfinder.mcp.server.registry.InMemoryMcpServerToolRegistry;
import com.flightpathfinder.mcp.server.registry.McpServerToolRegistry;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires server-side MCP runtime components.
 *
 * <p>The registry is assembled inside {@code pathfinder-mcp-server} so tool exposure stays owned by the
 * protocol server process instead of leaking bootstrap-side client discovery concerns back into the
 * server.
 */
@Configuration
@EnableConfigurationProperties(McpServerDatasourceProperties.class)
public class McpServerConfiguration {

    /**
     * Creates the in-process registry used by protocol handlers and dispatchers to discover available
     * tools.
     *
     * @param toolExecutors all server-side tool executors contributed by this module
     * @return registry backed by the current executor set
     */
    @Bean
    public McpServerToolRegistry mcpServerToolRegistry(List<McpToolExecutor> toolExecutors) {
        return new InMemoryMcpServerToolRegistry(toolExecutors);
    }
}

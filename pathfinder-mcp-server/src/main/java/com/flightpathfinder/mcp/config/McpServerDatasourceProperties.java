package com.flightpathfinder.mcp.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.datasource")
public record McpServerDatasourceProperties(
        @NotBlank(message = "pathfinder-mcp-server requires spring.datasource.url or PATHFINDER_MCP_DB_URL")
        String url,
        @NotBlank(message = "pathfinder-mcp-server requires spring.datasource.username or PATHFINDER_MCP_DB_USERNAME")
        String username,
        @NotBlank(message = "pathfinder-mcp-server requires spring.datasource.password or PATHFINDER_MCP_DB_PASSWORD")
        String password) {
}

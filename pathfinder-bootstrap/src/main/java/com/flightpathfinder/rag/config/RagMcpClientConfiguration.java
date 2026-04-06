package com.flightpathfinder.rag.config;

import com.flightpathfinder.rag.core.mcp.client.McpClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * 说明。
 */

@Configuration
@EnableConfigurationProperties(McpClientProperties.class)
public class RagMcpClientConfiguration {
}




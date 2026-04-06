package com.flightpathfinder.rag.config;

import com.flightpathfinder.rag.core.mcp.client.McpClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * 面向 RAG 的功能配置组件。
 */

@Configuration
@EnableConfigurationProperties(McpClientProperties.class)
public class RagMcpClientConfiguration {
}




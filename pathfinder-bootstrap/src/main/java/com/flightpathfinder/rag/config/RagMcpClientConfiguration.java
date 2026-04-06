package com.flightpathfinder.rag.config;

import com.flightpathfinder.rag.core.mcp.client.McpClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * 用于定义当前类型或方法在模块内的职责边界。
 */

@Configuration
@EnableConfigurationProperties(McpClientProperties.class)
public class RagMcpClientConfiguration {
}






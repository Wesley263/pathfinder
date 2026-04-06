package com.flightpathfinder.rag.core.mcp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 核心组件。
 */

@ConfigurationProperties(prefix = "pathfinder.rag.mcp.client")
public class McpClientProperties {

    private String baseUrl = "http://localhost:18081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}



package com.flightpathfinder.rag.core.mcp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 用于提供当前领域能力的默认实现。
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





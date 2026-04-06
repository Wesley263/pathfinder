package com.flightpathfinder.mcp.config;

import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import com.flightpathfinder.mcp.server.registry.InMemoryMcpServerToolRegistry;
import com.flightpathfinder.mcp.server.registry.McpServerToolRegistry;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 核心配置。
 *
 * 负责注册服务端工具注册表并装配基础数据源配置。
 */
@Configuration
@EnableConfigurationProperties(McpServerDatasourceProperties.class)
public class McpServerConfiguration {

    /**
     * 创建供协议处理器与分发器发现可用工具的进程内注册器。
     *
     * @param toolExecutors 本模块提供的全部服务端工具执行器
     * @return 由当前执行器集合驱动的注册器
     */
    @Bean
    public McpServerToolRegistry mcpServerToolRegistry(List<McpToolExecutor> toolExecutors) {
        return new InMemoryMcpServerToolRegistry(toolExecutors);
    }
}


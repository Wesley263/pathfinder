package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.List;
import java.util.Optional;

/**
 * 当前服务进程对外暴露的 MCP 工具目录。
 *
 * <p>该注册器刻意保持窄职责：只回答“有哪些工具”与“某工具由谁执行”。
 * 协议分发、依赖健康检查与业务执行不属于此抽象。
 */
public interface McpServerToolRegistry {

    /**
     * 列出 MCP 服务当前对外暴露的全部工具描述。
     *
     * @return 按注册顺序返回、供 {@code tools/list} 使用的工具描述
     */
    List<McpToolDescriptor> listTools();

    /**
     * 查找给定 toolId 对应的执行器。
     *
     * @param toolId MCP 工具 id，例如 {@code graph.path.search}
     * @return 若当前进程已注册该工具则返回匹配执行器
     */
    Optional<McpToolExecutor> findExecutor(String toolId);
}

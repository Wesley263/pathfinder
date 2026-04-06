package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.List;
import java.util.Optional;

/**
 * MCP 服务端工具注册表抽象。
 *
 * 负责维护工具描述与执行器的映射关系，
 * 协议分发、依赖健康检查与业务执行不属于此抽象。
 */
public interface McpServerToolRegistry {

    /**
     * 列出当前已注册的工具描述。
     *
     * @return 工具描述列表
     */
    List<McpToolDescriptor> listTools();

    /**
     * 按工具标识查找执行器。
     *
     * @param toolId 工具标识
     * @return 若当前进程已注册该工具则返回匹配执行器
     */
    Optional<McpToolExecutor> findExecutor(String toolId);
}

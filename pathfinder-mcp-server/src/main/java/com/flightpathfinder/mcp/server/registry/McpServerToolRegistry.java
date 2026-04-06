package com.flightpathfinder.mcp.server.registry;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.server.executor.McpToolExecutor;
import java.util.List;
import java.util.Optional;

/**
 * 说明。
 *
 * 说明。
 * 协议分发、依赖健康检查与业务执行不属于此抽象。
 */
public interface McpServerToolRegistry {

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    List<McpToolDescriptor> listTools();

    /**
     * 说明。
     *
     * @param toolId 参数说明。
     * @return 若当前进程已注册该工具则返回匹配执行器
     */
    Optional<McpToolExecutor> findExecutor(String toolId);
}

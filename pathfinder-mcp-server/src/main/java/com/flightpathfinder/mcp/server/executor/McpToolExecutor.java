package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;

/**
 * 每个服务端 MCP 工具都要实现的执行契约。
 *
 * <p>执行器保留在 {@code pathfinder-mcp-server} 内，
 * 因为每个工具都在服务端独立拥有数据访问与规则逻辑。
 * 引导侧仅通过 MCP 合约调用，不共享 Java 实现。
 */
public interface McpToolExecutor {

    /**
     * 描述通过 MCP 发现接口暴露的工具契约。
     *
     * @return 工具描述，包含 toolId、可读说明及输入/输出 schema
     */
    McpToolDescriptor descriptor();

    /**
     * 使用已解码的 MCP 请求参数执行一次工具调用。
     *
     * @param request 工具调用请求，包含目标 toolId 与结构化参数
     * @return 结构化工具结果，必要时包含业务级缺失态或非法请求态
     */
    McpToolCallResult execute(McpToolCallRequest request);
}

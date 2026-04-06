package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;

/**
 * MCP 工具执行器抽象。
 *
 * 每个工具执行器独立声明自身描述与执行逻辑，
 * 因为每个工具都在服务端独立拥有数据访问与规则逻辑。
 */
public interface McpToolExecutor {

    /**
     * 返回当前执行器声明的工具描述。
     *
     * @return MCP 工具描述
     */
    McpToolDescriptor descriptor();

    /**
     * 执行一次工具调用请求。
     *
     * @param request MCP 工具调用请求
     * @return 结构化工具结果，必要时包含业务级缺失态或非法请求态
     */
    McpToolCallResult execute(McpToolCallRequest request);
}

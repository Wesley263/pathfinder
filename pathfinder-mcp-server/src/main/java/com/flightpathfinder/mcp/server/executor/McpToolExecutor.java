package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;

/**
 * 说明。
 *
 * 说明。
 * 因为每个工具都在服务端独立拥有数据访问与规则逻辑。
 * 说明。
 */
public interface McpToolExecutor {

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    McpToolDescriptor descriptor();

    /**
     * 说明。
     *
     * @param request 参数说明。
     * @return 结构化工具结果，必要时包含业务级缺失态或非法请求态
     */
    McpToolCallResult execute(McpToolCallRequest request);
}

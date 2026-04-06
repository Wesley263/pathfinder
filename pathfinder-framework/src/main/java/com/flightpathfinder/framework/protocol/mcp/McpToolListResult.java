package com.flightpathfinder.framework.protocol.mcp;

import java.util.List;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record McpToolListResult(List<McpToolDescriptor> tools) {
}




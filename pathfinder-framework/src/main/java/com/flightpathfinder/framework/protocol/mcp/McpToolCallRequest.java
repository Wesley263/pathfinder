package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record McpToolCallRequest(String toolId, Map<String, Object> arguments) {
}




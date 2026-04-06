package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record McpToolDescriptor(
        String toolId,
        String displayName,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema) {
}




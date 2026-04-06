package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 说明。
 */
public record McpToolDescriptor(
        String toolId,
        String displayName,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema) {
}




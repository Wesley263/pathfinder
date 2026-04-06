package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 说明。
 */
public record McpToolCallResult(
        String toolId,
        boolean success,
        String content,
        Map<String, Object> structuredContent,
        String errorMessage) {
}




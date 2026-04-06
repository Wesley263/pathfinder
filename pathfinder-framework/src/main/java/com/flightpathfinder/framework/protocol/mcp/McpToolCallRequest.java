package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 说明。
 */
public record McpToolCallRequest(String toolId, Map<String, Object> arguments) {
}




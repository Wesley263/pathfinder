package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;

public record McpToolCallRequest(String toolId, Map<String, Object> arguments) {
}


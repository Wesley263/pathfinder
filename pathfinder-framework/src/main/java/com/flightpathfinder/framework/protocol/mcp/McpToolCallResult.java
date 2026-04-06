package com.flightpathfinder.framework.protocol.mcp;

import java.util.Map;
/**
 * 用于定义当前类型或方法在模块内的职责边界。
 */
public record McpToolCallResult(
        String toolId,
        boolean success,
        String content,
        Map<String, Object> structuredContent,
        String errorMessage) {
}






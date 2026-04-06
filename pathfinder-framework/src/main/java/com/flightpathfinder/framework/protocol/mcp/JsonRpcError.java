package com.flightpathfinder.framework.protocol.mcp;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record JsonRpcError(int code, String message) {
}




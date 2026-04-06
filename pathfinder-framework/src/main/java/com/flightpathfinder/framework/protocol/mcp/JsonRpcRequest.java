package com.flightpathfinder.framework.protocol.mcp;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record JsonRpcRequest<T>(String jsonrpc, Object id, String method, T params) {
}




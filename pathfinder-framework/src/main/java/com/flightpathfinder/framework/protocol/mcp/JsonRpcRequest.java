package com.flightpathfinder.framework.protocol.mcp;
/**
 * 说明。
 */
public record JsonRpcRequest<T>(String jsonrpc, Object id, String method, T params) {
}




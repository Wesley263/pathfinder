package com.flightpathfinder.framework.protocol.mcp;

public record JsonRpcRequest<T>(String jsonrpc, Object id, String method, T params) {
}


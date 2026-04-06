package com.flightpathfinder.framework.protocol.mcp;
/**
 * 面向 MCP 协议层的通用模型。
 */
public record JsonRpcResponse<T>(String jsonrpc, Object id, T result, JsonRpcError error) {

    public static <T> JsonRpcResponse<T> success(Object id, T result) {
        return new JsonRpcResponse<>("2.0", id, result, null);
    }

    public static <T> JsonRpcResponse<T> error(Object id, JsonRpcError error) {
        return new JsonRpcResponse<>("2.0", id, null, error);
    }
}




package com.flightpathfinder.framework.protocol.mcp;
/**
 * 用于定义当前类型或方法在模块内的职责边界。
 */
public record JsonRpcResponse<T>(String jsonrpc, Object id, T result, JsonRpcError error) {

    public static <T> JsonRpcResponse<T> success(Object id, T result) {
        return new JsonRpcResponse<>("2.0", id, result, null);
    }

    public static <T> JsonRpcResponse<T> error(Object id, JsonRpcError error) {
        return new JsonRpcResponse<>("2.0", id, null, error);
    }
}






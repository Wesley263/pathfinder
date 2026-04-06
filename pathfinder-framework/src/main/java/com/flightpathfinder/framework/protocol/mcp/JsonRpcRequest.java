package com.flightpathfinder.framework.protocol.mcp;
/**
 * 用于定义当前类型或方法在模块内的职责边界。
 */
public record JsonRpcRequest<T>(String jsonrpc, Object id, String method, T params) {
}






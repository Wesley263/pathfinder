package com.flightpathfinder.framework.protocol.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class JsonRpcResponseTest {

    @Test
    void success_shouldBuildStandardJsonRpcSuccessPayload() {
        JsonRpcResponse<String> response = JsonRpcResponse.success("id-1", "ok");

        assertEquals("2.0", response.jsonrpc());
        assertEquals("id-1", response.id());
        assertEquals("ok", response.result());
        assertNull(response.error());
    }

    @Test
    void error_shouldBuildStandardJsonRpcErrorPayload() {
        JsonRpcError error = new JsonRpcError(-32600, "invalid request");
        JsonRpcResponse<Void> response = JsonRpcResponse.error("id-2", error);

        assertEquals("2.0", response.jsonrpc());
        assertEquals("id-2", response.id());
        assertNull(response.result());
        assertEquals(error, response.error());
    }
}


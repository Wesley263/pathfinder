package com.flightpathfinder.rag.core.mcp.client;

import com.flightpathfinder.framework.protocol.mcp.JsonRpcRequest;
import com.flightpathfinder.framework.protocol.mcp.JsonRpcResponse;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.framework.protocol.mcp.McpToolListResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
/**
 * 核心组件。
 */

@Component
public class HttpMcpClient implements McpClient {

    private final RestClient restClient;

    public HttpMcpClient(RestClient.Builder restClientBuilder, McpClientProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public List<McpToolDescriptor> listTools() {
        try {
            JsonRpcResponse<McpToolListResult> response = restClient.post()
                    .uri("/mcp")
                    .body(new JsonRpcRequest<>("2.0", UUID.randomUUID().toString(), "tools/list", Map.of()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || response.error() != null || response.result() == null) {
                return List.of();
            }
            return response.result().tools();
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    @Override
    public McpToolCallResult callTool(McpToolCallRequest request) {
        try {
            JsonRpcResponse<McpToolCallResult> response = restClient.post()
                    .uri("/mcp")
                    .body(new JsonRpcRequest<>("2.0", UUID.randomUUID().toString(), "tools/call", request))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                return failureResult(request.toolId(), "MCP server returned no response");
            }
            if (response.error() != null) {
                return failureResult(request.toolId(), response.error().message());
            }
            return response.result();
        } catch (RestClientException ex) {
            return failureResult(request.toolId(), ex.getMessage());
        }
    }

    private McpToolCallResult failureResult(String toolId, String errorMessage) {
        return new McpToolCallResult(toolId, false, null, Map.of(), errorMessage);
    }
}


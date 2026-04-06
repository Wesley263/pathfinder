package com.flightpathfinder.infra.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.infra.ai.model.ModelCapability;
import com.flightpathfinder.infra.ai.model.ModelProvider;
import com.flightpathfinder.infra.ai.model.ModelRoute;
import com.flightpathfinder.infra.ai.model.ModelRoutingService;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * OpenAI 兼容协议聊天服务实现。
 *
 * 基于模型路由配置调用兼容接口，并提供同步与流式两种对话能力。
 */
@Service
@Primary
public class OpenAiCompatibleChatService implements ChatService, StreamingChatService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Set<ModelProvider> SUPPORTED_PROVIDERS = Set.of(
            ModelProvider.OPENAI_COMPATIBLE,
            ModelProvider.SILICON_FLOW,
            ModelProvider.BAILIAN);

    private final ModelRoutingService modelRoutingService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public OpenAiCompatibleChatService(ModelRoutingService modelRoutingService,
                                       ObjectMapper objectMapper) {
        this.modelRoutingService = modelRoutingService;
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ModelRoute route = selectRoute(ModelCapability.CHAT).orElse(null);
        if (route == null) {
            return placeholder("AI chat route is not configured yet.");
        }
        if (request == null || isBlank(request.userPrompt())) {
            return placeholderForRoute(route, "AI chat request is empty.");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(buildChatPayload(route, request, false));
            Request.Builder builder = new Request.Builder()
                    .url(resolveUrl(route))
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            if (!isBlank(route.apiKey())) {
                builder.header("Authorization", "Bearer " + route.apiKey());
            }
            try (Response response = clientFor(route).newCall(builder.build()).execute()) {
                if (!response.isSuccessful()) {
                    return placeholderForRoute(route, "Chat provider call failed with status " + response.code());
                }
                String body = readBody(response.body());
                String content = extractChatContent(objectMapper.readTree(body));
                if (isBlank(content)) {
                    return placeholderForRoute(route, "Chat provider returned an empty content payload.");
                }
                return new ChatResponse(content, route.modelName(), false);
            }
        } catch (Exception exception) {
            return placeholderForRoute(route, "Chat provider call failed.");
        }
    }

    @Override
    public void stream(ChatRequest request, StreamCallback callback) {
        StreamCallback safeCallback = callback == null ? new StreamCallback() {
            @Override
            public void onChunk(String chunk) {
            }
        } : callback;
        ModelRoute route = selectRoute(ModelCapability.STREAMING).orElse(null);
        if (route == null) {
            safeCallback.onChunk("AI streaming route is not configured yet.");
            safeCallback.onComplete();
            return;
        }
        if (request == null || isBlank(request.userPrompt())) {
            safeCallback.onChunk("AI chat request is empty.");
            safeCallback.onComplete();
            return;
        }

        try {
            String requestBody = objectMapper.writeValueAsString(buildChatPayload(route, request, true));
            Request.Builder builder = new Request.Builder()
                    .url(resolveUrl(route))
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream");
            if (!isBlank(route.apiKey())) {
                builder.header("Authorization", "Bearer " + route.apiKey());
            }
            try (Response response = clientFor(route).newCall(builder.build()).execute()) {
                if (!response.isSuccessful()) {
                    safeCallback.onChunk("Streaming provider returned status " + response.code());
                    safeCallback.onComplete();
                    return;
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    safeCallback.onComplete();
                    return;
                }
                BufferedSource source = responseBody.source();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null || line.isBlank() || !line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    String chunk = extractStreamChunk(objectMapper.readTree(payload));
                    if (!isBlank(chunk)) {
                        safeCallback.onChunk(chunk);
                    }
                }
                safeCallback.onComplete();
            }
        } catch (Exception exception) {
            safeCallback.onError(exception);
        }
    }

    private Optional<ModelRoute> selectRoute(ModelCapability capability) {
        return modelRoutingService.selectPrimary(capability)
                .filter(route -> SUPPORTED_PROVIDERS.contains(route.providerType()))
                .filter(route -> !isBlank(route.baseUrl()))
                .filter(route -> !isBlank(route.modelName()))
                .filter(route -> !isBlank(route.endpoint()));
    }

    private OkHttpClient clientFor(ModelRoute route) {
        long timeoutMs = route.timeoutMs() == null || route.timeoutMs() <= 0 ? 30000L : route.timeoutMs();
        return okHttpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private Map<String, Object> buildChatPayload(ModelRoute route, ChatRequest request, boolean stream) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (!isBlank(request.systemPrompt())) {
            messages.add(message("system", request.systemPrompt()));
        }
        messages.add(message("user", request.userPrompt()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", route.modelName());
        payload.put("messages", messages);
        payload.put("stream", stream);
        if (route.temperature() != null) {
            payload.put("temperature", route.temperature());
        }
        if (route.maxTokens() != null) {
            payload.put("max_tokens", route.maxTokens());
        }
        return payload;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private String resolveUrl(ModelRoute route) {
        String baseUrl = route.baseUrl().endsWith("/")
                ? route.baseUrl().substring(0, route.baseUrl().length() - 1)
                : route.baseUrl();
        String endpoint = route.endpoint().startsWith("/") ? route.endpoint() : "/" + route.endpoint();
        return baseUrl + endpoint;
    }

    private String extractChatContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode firstChoice = choices.get(0);
        String content = firstChoice.path("message").path("content").asText("");
        if (!content.isBlank()) {
            return content.trim();
        }
        return firstChoice.path("delta").path("content").asText("").trim();
    }

    private String extractStreamChunk(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode firstChoice = choices.get(0);
        String content = firstChoice.path("delta").path("content").asText("");
        if (!content.isBlank()) {
            return content;
        }
        return firstChoice.path("message").path("content").asText("");
    }

    private String readBody(ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
    }

    private ChatResponse placeholder(String message) {
        return new ChatResponse(message, "unconfigured", true);
    }

    private ChatResponse placeholderForRoute(ModelRoute route, String message) {
        return new ChatResponse(message, route.modelName(), true);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


package com.flightpathfinder.infra.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.infra.ai.model.ModelCapability;
import com.flightpathfinder.infra.ai.model.ModelProvider;
import com.flightpathfinder.infra.ai.model.ModelRoute;
import com.flightpathfinder.infra.ai.model.ModelRoutingService;
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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 兼容 OpenAI 协议的向量生成客户端实现。
 *
 * <p>用于对接百炼、SiliconFlow 等兼容接口的 embedding 服务。
 */
@Service
@Primary
public class OpenAiCompatibleEmbeddingService implements EmbeddingService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Set<ModelProvider> SUPPORTED_PROVIDERS = Set.of(
            ModelProvider.OPENAI_COMPATIBLE,
            ModelProvider.SILICON_FLOW,
            ModelProvider.BAILIAN);

    private final ModelRoutingService modelRoutingService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public OpenAiCompatibleEmbeddingService(ModelRoutingService modelRoutingService,
                                            ObjectMapper objectMapper) {
        this.modelRoutingService = modelRoutingService;
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient();
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ModelRoute route = selectRoute().orElse(null);
        if (route == null) {
            return List.of();
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", route.modelName());
            payload.put("input", text);
            String requestBody = objectMapper.writeValueAsString(payload);

            Request.Builder builder = new Request.Builder()
                    .url(resolveUrl(route))
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            if (route.apiKey() != null && !route.apiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + route.apiKey());
            }

            try (Response response = clientFor(route).newCall(builder.build()).execute()) {
                if (!response.isSuccessful()) {
                    return List.of();
                }
                String body = response.body() == null ? "" : response.body().string();
                return extractEmbedding(body);
            }
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Optional<ModelRoute> selectRoute() {
        return modelRoutingService.selectPrimary(ModelCapability.EMBEDDING)
                .filter(route -> SUPPORTED_PROVIDERS.contains(route.providerType()))
                .filter(route -> route.baseUrl() != null && !route.baseUrl().isBlank())
                .filter(route -> route.modelName() != null && !route.modelName().isBlank())
                .filter(route -> route.endpoint() != null && !route.endpoint().isBlank());
    }

    private OkHttpClient clientFor(ModelRoute route) {
        long timeoutMs = route.timeoutMs() == null || route.timeoutMs() <= 0 ? 30000L : route.timeoutMs();
        return okHttpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private String resolveUrl(ModelRoute route) {
        String baseUrl = route.baseUrl().endsWith("/")
                ? route.baseUrl().substring(0, route.baseUrl().length() - 1)
                : route.baseUrl();
        String endpoint = route.endpoint().startsWith("/") ? route.endpoint() : "/" + route.endpoint();
        return baseUrl + endpoint;
    }

    private List<Float> extractEmbedding(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return List.of();
        }
        JsonNode embedding = data.get(0).path("embedding");
        if (!embedding.isArray()) {
            return List.of();
        }
        List<Float> values = new ArrayList<>(embedding.size());
        for (JsonNode node : embedding) {
            values.add((float) node.asDouble());
        }
        return List.copyOf(values);
    }
}
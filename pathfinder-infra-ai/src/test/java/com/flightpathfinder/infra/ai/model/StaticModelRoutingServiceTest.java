package com.flightpathfinder.infra.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.infra.ai.config.AiModelProperties;
import com.flightpathfinder.infra.ai.config.AiModelProperties.ProviderEndpoints;
import com.flightpathfinder.infra.ai.config.AiModelProperties.ProviderProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * StaticModelRoutingService 单元测试。
 */
class StaticModelRoutingServiceTest {

    @Test
    void selectPrimary_chatShouldReturnConfiguredRoute() {
        AiModelProperties properties = buildProperties();
        StaticModelRoutingService service = new StaticModelRoutingService(properties);

        ModelRoute route = service.selectPrimary(ModelCapability.CHAT).orElseThrow();

        assertEquals("chat", route.routeName());
        assertEquals("provider-a", route.providerId());
        assertEquals(ModelProvider.OPENAI_COMPATIBLE, route.providerType());
        assertEquals("chat-model", route.modelName());
        assertEquals("https://api.example.com", route.baseUrl());
        assertEquals("/v1/chat/completions", route.endpoint());
        assertTrue(route.capabilities().contains(ModelCapability.CHAT));
        assertTrue(route.capabilities().contains(ModelCapability.STREAMING));
        assertEquals(12345L, route.timeoutMs());
        assertEquals(0.3D, route.temperature());
        assertEquals(2048, route.maxTokens());
        assertEquals(true, route.streamingEnabled());
        assertNull(route.dimension());
    }

    @Test
    void selectPrimary_streamingShouldReturnEmptyWhenDisabled() {
        AiModelProperties properties = buildProperties();
        properties.getChat().setStreamingEnabled(false);
        StaticModelRoutingService service = new StaticModelRoutingService(properties);

        assertTrue(service.selectPrimary(ModelCapability.STREAMING).isEmpty());
    }

    @Test
    void selectPrimary_embeddingShouldReturnConfiguredRoute() {
        AiModelProperties properties = buildProperties();
        StaticModelRoutingService service = new StaticModelRoutingService(properties);

        ModelRoute route = service.selectPrimary(ModelCapability.EMBEDDING).orElseThrow();

        assertEquals("embedding", route.routeName());
        assertEquals("provider-a", route.providerId());
        assertEquals("embedding-model", route.modelName());
        assertEquals("/v1/embeddings", route.endpoint());
        assertEquals(1, route.capabilities().size());
        assertTrue(route.capabilities().contains(ModelCapability.EMBEDDING));
        assertEquals(23456L, route.timeoutMs());
        assertEquals(1536, route.dimension());
        assertEquals(false, route.streamingEnabled());
        assertNull(route.temperature());
        assertNull(route.maxTokens());
    }

    @Test
    void selectPrimary_shouldReturnEmptyWhenProviderDisabledOrCapabilityUnsupported() {
        AiModelProperties properties = buildProperties();
        properties.getProviders().get("provider-a").setEnabled(false);
        StaticModelRoutingService service = new StaticModelRoutingService(properties);

        assertTrue(service.selectPrimary(ModelCapability.CHAT).isEmpty());
        assertTrue(service.selectPrimary(ModelCapability.EMBEDDING).isEmpty());
        assertTrue(service.selectPrimary(ModelCapability.RERANK).isEmpty());
    }

    private AiModelProperties buildProperties() {
        AiModelProperties properties = new AiModelProperties();

        ProviderEndpoints endpoints = new ProviderEndpoints();
        endpoints.setChat("/v1/chat/completions");
        endpoints.setEmbedding("/v1/embeddings");

        ProviderProperties provider = new ProviderProperties();
        provider.setType(ModelProvider.OPENAI_COMPATIBLE);
        provider.setEnabled(true);
        provider.setBaseUrl("https://api.example.com");
        provider.setApiKey("test-key");
        provider.setEndpoints(endpoints);

        Map<String, ProviderProperties> providers = new LinkedHashMap<>();
        providers.put("provider-a", provider);
        properties.setProviders(providers);

        properties.getChat().setProvider("provider-a");
        properties.getChat().setModelName("chat-model");
        properties.getChat().setEnabled(true);
        properties.getChat().setStreamingEnabled(true);
        properties.getChat().setTimeoutMs(12345L);
        properties.getChat().setTemperature(0.3D);
        properties.getChat().setMaxTokens(2048);

        properties.getEmbedding().setProvider("provider-a");
        properties.getEmbedding().setModelName("embedding-model");
        properties.getEmbedding().setEnabled(true);
        properties.getEmbedding().setTimeoutMs(23456L);
        properties.getEmbedding().setDimension(1536);

        return properties;
    }
}


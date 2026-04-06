package com.flightpathfinder.infra.ai.model;

import com.flightpathfinder.infra.ai.config.AiModelProperties;
import com.flightpathfinder.infra.ai.config.AiModelProperties.ProviderProperties;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 仅基于配置的静态能力路由选择器。
 *
 * <p>Pathfinder 2.0 当前保持显式 provider 选择：chat 与 embedding 分别绑定
 * 一个已配置的 provider/model 组合，不执行动态多模型路由。
 */
@Component
public class StaticModelRoutingService implements ModelRoutingService {

    private final AiModelProperties properties;

    public StaticModelRoutingService(AiModelProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<ModelRoute> selectPrimary(ModelCapability capability) {
        return switch (capability) {
            case CHAT -> resolveChat(false);
            case STREAMING -> resolveChat(true);
            case EMBEDDING -> resolveEmbedding();
            case RERANK -> Optional.empty();
        };
    }

    private Optional<ModelRoute> resolveChat(boolean streamingRequested) {
        AiModelProperties.ChatProperties chat = properties.getChat();
        if (!chat.isEnabled()) {
            return Optional.empty();
        }
        if (streamingRequested && !chat.isStreamingEnabled()) {
            return Optional.empty();
        }
        return resolveProvider(chat.getProvider())
                .map(provider -> new ModelRoute(
                        "chat",
                        chat.getProvider(),
                        provider.getType(),
                        chat.getModelName(),
                        provider.getBaseUrl(),
                        provider.getApiKey(),
                        provider.getEndpoints().getChat(),
                        resolveChatCapabilities(chat),
                        chat.getTimeoutMs(),
                        chat.getTemperature(),
                        chat.getMaxTokens(),
                        chat.isStreamingEnabled(),
                        null));
    }

    private Optional<ModelRoute> resolveEmbedding() {
        AiModelProperties.EmbeddingProperties embedding = properties.getEmbedding();
        if (!embedding.isEnabled()) {
            return Optional.empty();
        }
        return resolveProvider(embedding.getProvider())
                .map(provider -> new ModelRoute(
                        "embedding",
                        embedding.getProvider(),
                        provider.getType(),
                        embedding.getModelName(),
                        provider.getBaseUrl(),
                        provider.getApiKey(),
                        provider.getEndpoints().getEmbedding(),
                        EnumSet.of(ModelCapability.EMBEDDING),
                        embedding.getTimeoutMs(),
                        null,
                        null,
                        false,
                        embedding.getDimension()));
    }

    private Optional<ProviderProperties> resolveProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return Optional.empty();
        }
        ProviderProperties provider = properties.getProviders().get(providerId);
        if (provider == null || !provider.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(provider);
    }

    private Set<ModelCapability> resolveChatCapabilities(AiModelProperties.ChatProperties chat) {
        if (chat.isStreamingEnabled()) {
            return EnumSet.of(ModelCapability.CHAT, ModelCapability.STREAMING);
        }
        return EnumSet.of(ModelCapability.CHAT);
    }
}
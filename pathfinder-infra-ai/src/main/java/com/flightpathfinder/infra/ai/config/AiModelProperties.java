package com.flightpathfinder.infra.ai.config;

import com.flightpathfinder.infra.ai.model.ModelProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI infrastructure configuration split into provider connectivity and capability selection.
 * <p>
 * Providers describe how to reach a vendor endpoint, while chat and embedding describe
 * which configured provider/model pair should currently serve each capability.
 */
@ConfigurationProperties(prefix = "pathfinder.ai")
public class AiModelProperties {

    private Map<String, ProviderProperties> providers = new LinkedHashMap<>();
    private ChatProperties chat = new ChatProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();

    public Map<String, ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderProperties> providers) {
        this.providers = providers;
    }

    public ChatProperties getChat() {
        return chat;
    }

    public void setChat(ChatProperties chat) {
        this.chat = chat;
    }

    public EmbeddingProperties getEmbedding() {
        return embedding;
    }

    public void setEmbedding(EmbeddingProperties embedding) {
        this.embedding = embedding;
    }

    /**
     * Connectivity and authentication settings for one upstream AI provider.
     */
    public static class ProviderProperties {

        private ModelProvider type = ModelProvider.MOCK;
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private ProviderEndpoints endpoints = new ProviderEndpoints();

        public ModelProvider getType() {
            return type;
        }

        public void setType(ModelProvider type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public ProviderEndpoints getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(ProviderEndpoints endpoints) {
            this.endpoints = endpoints;
        }
    }

    /**
     * Endpoint suffixes for provider-specific capability calls.
     */
    public static class ProviderEndpoints {

        private String chat;
        private String embedding;

        public String getChat() {
            return chat;
        }

        public void setChat(String chat) {
            this.chat = chat;
        }

        public String getEmbedding() {
            return embedding;
        }

        public void setEmbedding(String embedding) {
            this.embedding = embedding;
        }
    }

    /**
     * Selection and runtime options for chat and streaming chat generation.
     */
    public static class ChatProperties {

        private String provider = "siliconflow";
        private String modelName = "placeholder-chat-model";
        private boolean enabled = true;
        private boolean streamingEnabled = true;
        private long timeoutMs = 30000L;
        private double temperature = 0.2D;
        private int maxTokens = 4096;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isStreamingEnabled() {
            return streamingEnabled;
        }

        public void setStreamingEnabled(boolean streamingEnabled) {
            this.streamingEnabled = streamingEnabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    /**
     * Selection and runtime options for embedding generation.
     */
    public static class EmbeddingProperties {

        private String provider = "bailian";
        private String modelName = "placeholder-embedding-model";
        private boolean enabled = true;
        private long timeoutMs = 30000L;
        private Integer dimension;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Integer getDimension() {
            return dimension;
        }

        public void setDimension(Integer dimension) {
            this.dimension = dimension;
        }
    }
}

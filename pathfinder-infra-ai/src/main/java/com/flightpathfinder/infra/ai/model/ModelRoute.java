package com.flightpathfinder.infra.ai.model;

import java.util.Set;

/**
 * Resolved model selection for one capability at runtime.
 */
public record ModelRoute(
        String routeName,
        String providerId,
        ModelProvider providerType,
        String modelName,
        String baseUrl,
        String apiKey,
        String endpoint,
        Set<ModelCapability> capabilities,
        Long timeoutMs,
        Double temperature,
        Integer maxTokens,
        Boolean streamingEnabled,
        Integer dimension) {
}

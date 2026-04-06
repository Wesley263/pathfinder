package com.flightpathfinder.infra.ai.model;

import java.util.Set;

/**
 * 单个能力在运行时解析后的模型路由结果。
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

package com.flightpathfinder.infra.ai.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
/**
 * 向量生成能力的空实现。
 */

@Component
@ConditionalOnMissingBean(EmbeddingService.class)
public class NoopEmbeddingService implements EmbeddingService {

    @Override
    public java.util.List<Float> embed(String text) {
        return java.util.List.of();
    }
}

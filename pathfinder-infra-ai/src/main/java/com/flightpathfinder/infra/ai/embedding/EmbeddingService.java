package com.flightpathfinder.infra.ai.embedding;

import java.util.List;
/**
 * 向量生成能力接口定义。
 */
public interface EmbeddingService {

    List<Float> embed(String text);
}



package com.flightpathfinder.infra.ai.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Float> embed(String text);
}


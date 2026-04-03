package com.flightpathfinder.infra.ai.chat;

import java.util.Map;

public record ChatRequest(String userPrompt, String systemPrompt, Map<String, Object> metadata) {
}


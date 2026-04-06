package com.flightpathfinder.infra.ai.chat;

import java.util.Map;
/**
 * 说明。
 */
public record ChatRequest(String userPrompt, String systemPrompt, Map<String, Object> metadata) {
}




package com.flightpathfinder.rag.core.intent;

public record ResolvedIntent(
        String intentId,
        String intentName,
        IntentKind kind,
        double score,
        String question,
        String fullPath,
        String description,
        String mcpToolId,
        String mcpToolName,
        String kbCollectionName,
        Integer kbTopK) {
}

package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record IntentNode(
        String id,
        String name,
        String description,
        IntentLevel level,
        IntentKind kind,
        List<String> keywords,
        List<String> examples,
        List<String> aliases,
        String fullPath,
        List<IntentNode> children,
        String mcpToolId,
        String collectionName,
        Integer topK) {

    public IntentNode {
        id = Objects.requireNonNull(id, "id cannot be null");
        name = Objects.requireNonNull(name, "name cannot be null");
        description = Objects.requireNonNull(description, "description cannot be null");
        level = Objects.requireNonNull(level, "level cannot be null");
        keywords = List.copyOf(keywords == null ? List.of() : keywords);
        examples = List.copyOf(examples == null ? List.of() : examples);
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        children = List.copyOf(children == null ? List.of() : children);
        if (level == IntentLevel.TOPIC && kind == null) {
            throw new IllegalArgumentException("topic intent node must declare kind");
        }
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean matchesId(String candidateId) {
        if (candidateId == null || candidateId.isBlank()) {
            return false;
        }
        String normalized = normalize(candidateId);
        if (normalize(id).equals(normalized)) {
            return true;
        }
        if (mcpToolId != null && normalize(mcpToolId).equals(normalized)) {
            return true;
        }
        return aliases.stream().map(IntentNode::normalize).anyMatch(normalized::equals);
    }

    static IntentNode domain(String id, String name, String description, String fullPath, List<IntentNode> children) {
        return new IntentNode(id, name, description, IntentLevel.DOMAIN, null,
                List.of(), List.of(), List.of(), fullPath, children, null, null, null);
    }

    static IntentNode category(String id, String name, String description, String fullPath, List<IntentNode> children) {
        return new IntentNode(id, name, description, IntentLevel.CATEGORY, null,
                List.of(), List.of(), List.of(), fullPath, children, null, null, null);
    }

    static IntentNode mcpTopic(String id,
                               String name,
                               String description,
                               String fullPath,
                               String mcpToolId,
                               List<String> keywords,
                               List<String> aliases,
                               List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.MCP,
                keywords, examples, aliases, fullPath, List.of(), mcpToolId, null, null);
    }

    static IntentNode kbTopic(String id,
                              String name,
                              String description,
                              String fullPath,
                              String collectionName,
                              Integer topK,
                              List<String> keywords,
                              List<String> aliases,
                              List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.KB,
                keywords, examples, aliases, fullPath, List.of(), null, collectionName, topK);
    }

    static IntentNode systemTopic(String id,
                                  String name,
                                  String description,
                                  String fullPath,
                                  List<String> keywords,
                                  List<String> aliases,
                                  List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.SYSTEM,
                keywords, examples, aliases, fullPath, List.of(), null, null, null);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

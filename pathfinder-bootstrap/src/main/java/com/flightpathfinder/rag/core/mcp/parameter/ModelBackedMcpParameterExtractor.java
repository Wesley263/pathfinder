package com.flightpathfinder.rag.core.mcp.parameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.infra.ai.chat.ChatRequest;
import com.flightpathfinder.infra.ai.chat.ChatResponse;
import com.flightpathfinder.infra.ai.chat.ChatService;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于模型的 MCP 参数提取器，复用规则提取器中的 schema 校验器。
 */
@Service
@Primary
public class ModelBackedMcpParameterExtractor implements McpParameterExtractor {

    private final DefaultMcpParameterExtractor fallbackExtractor;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ModelBackedMcpParameterExtractor(DefaultMcpParameterExtractor fallbackExtractor,
                                            ChatService chatService,
                                            ObjectMapper objectMapper) {
        this.fallbackExtractor = fallbackExtractor;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpParameterExtractionResult extract(RewriteResult rewriteResult,
                                                ResolvedIntent resolvedIntent,
                                                McpToolDescriptor toolDescriptor) {
        McpParameterExtractionResult fallback = fallbackExtractor.extract(rewriteResult, resolvedIntent, toolDescriptor);
        if (resolvedIntent == null || toolDescriptor == null) {
            return fallback;
        }

        try {
            Map<String, Object> modelParameters = extractWithModel(rewriteResult, resolvedIntent, toolDescriptor);
            if (modelParameters.isEmpty()) {
                return fallback;
            }

            Map<String, Object> mergedParameters = new LinkedHashMap<>(fallback.parameters());
            mergedParameters.putAll(modelParameters);
            String syntheticQuestion = toSyntheticExtractionText(toolDescriptor.toolId(), mergedParameters);
            if (syntheticQuestion.isBlank()) {
                return fallback;
            }

            RewriteResult syntheticRewrite = rewriteResult == null
                    ? new RewriteResult(syntheticQuestion, List.of(syntheticQuestion), syntheticQuestion, List.of(syntheticQuestion))
                    : new RewriteResult(
                            rewriteResult.rewrittenQuestion(),
                            rewriteResult.subQuestions(),
                            syntheticQuestion,
                            List.of(syntheticQuestion));
            ResolvedIntent syntheticIntent = new ResolvedIntent(
                    resolvedIntent.intentId(),
                    resolvedIntent.intentName(),
                    resolvedIntent.kind(),
                    resolvedIntent.score(),
                    syntheticQuestion,
                    resolvedIntent.fullPath(),
                    resolvedIntent.description(),
                    resolvedIntent.mcpToolId(),
                    resolvedIntent.mcpToolName(),
                    resolvedIntent.kbCollectionName(),
                    resolvedIntent.kbTopK());
            McpParameterExtractionResult modelValidated = fallbackExtractor.extract(syntheticRewrite, syntheticIntent, toolDescriptor);
            return modelValidated.ready() ? modelValidated : fallback;
        } catch (Exception exception) {
            return fallback;
        }
    }

    private Map<String, Object> extractWithModel(RewriteResult rewriteResult,
                                                 ResolvedIntent resolvedIntent,
                                                 McpToolDescriptor toolDescriptor) throws Exception {
        String source = resolvedIntent.question() != null && !resolvedIntent.question().isBlank()
                ? resolvedIntent.question()
                : rewriteResult == null ? "" : rewriteResult.routingQuestion();
        if (source.isBlank()) {
            return Map.of();
        }

        ChatResponse response = chatService.chat(new ChatRequest(
                buildUserPrompt(source, toolDescriptor),
                buildSystemPrompt(toolDescriptor),
                Map.of("stage", "mcp-parameter-extract", "toolId", toolDescriptor.toolId())));
        if (response == null || response.placeholder() || response.content() == null || response.content().isBlank()) {
            return Map.of();
        }
        return parseParameterMap(response.content());
    }

    private String buildSystemPrompt(McpToolDescriptor toolDescriptor) throws Exception {
        return "You extract MCP tool parameters from a user question. "
                + "Return JSON object only. Use only fields defined by the input schema. "
                + "Do not invent values that are not implied by the user question. "
                + "Tool: " + toolDescriptor.toolId() + "\nSchema:\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolDescriptor.inputSchema());
    }

    private String buildUserPrompt(String source, McpToolDescriptor toolDescriptor) {
        return "User question:\n" + source + "\n\nReturn a JSON object for tool " + toolDescriptor.toolId() + '.';
    }

    private Map<String, Object> parseParameterMap(String raw) throws Exception {
        String cleaned = stripMarkdownCodeFence(raw);
        JsonNode root = objectMapper.readTree(cleaned);
        if (!root.isObject()) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                return;
            }
            if (value.isTextual()) {
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    values.put(entry.getKey(), text);
                }
                return;
            }
            if (value.isNumber()) {
                values.put(entry.getKey(), value.numberValue());
                return;
            }
            if (value.isArray()) {
                List<String> arrayValues = new ArrayList<>();
                value.forEach(item -> {
                    String text = item.asText("").trim();
                    if (!text.isBlank()) {
                        arrayValues.add(text);
                    }
                });
                if (!arrayValues.isEmpty()) {
                    values.put(entry.getKey(), arrayValues);
                }
                return;
            }
            values.put(entry.getKey(), value.toString());
        });
        return values;
    }

    private String toSyntheticExtractionText(String toolId, Map<String, Object> values) {
        return switch (toolId) {
            case "graph.path.search" -> graphPathText(values);
            case "flight.search" -> flightSearchText(values);
            case "price.lookup" -> priceLookupText(values);
            case "visa.check" -> visaCheckText(values);
            case "city.cost" -> cityCostText(values);
            case "risk.evaluate" -> riskEvaluateText(values);
            default -> "";
        };
    }

    private String graphPathText(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        appendRoute(parts, values.get("origin"), values.get("destination"));
        append(parts, "预算 " + stringValue(values.get("maxBudget")));
        append(parts, "stopoverDays=" + stringValue(values.get("stopoverDays")));
        append(parts, "maxSegments=" + stringValue(values.get("maxSegments")));
        append(parts, "topK=" + stringValue(values.get("topK")));
        return String.join(" ", parts).trim();
    }

    private String flightSearchText(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        appendRoute(parts, values.get("origin"), values.get("destination"));
        append(parts, stringValue(values.get("date")));
        append(parts, "flexibilityDays=" + stringValue(values.get("flexibilityDays")));
        append(parts, "topK=" + stringValue(values.get("topK")));
        return String.join(" ", parts).trim();
    }

    private String priceLookupText(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        append(parts, joinAny(values.get("cityPairs"), ";"));
        append(parts, stringValue(values.get("date")));
        return String.join(" ", parts).trim();
    }

    private String visaCheckText(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        append(parts, joinAny(values.get("countryCodes"), ","));
        append(parts, "passportCountry=" + stringValue(values.get("passportCountry")));
        append(parts, "stayDays=" + stringValue(values.get("stayDays")));
        return String.join(" ", parts).trim();
    }

    private String cityCostText(Map<String, Object> values) {
        return joinAny(values.get("iataCodes"), ",");
    }

    private String riskEvaluateText(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        append(parts, "via " + stringValue(values.get("hubAirport")) + " 中转");
        List<String> airlines = new ArrayList<>();
        if (!stringValue(values.get("firstAirline")).isBlank()) {
            airlines.add(stringValue(values.get("firstAirline")));
        }
        if (!stringValue(values.get("secondAirline")).isBlank()) {
            airlines.add(stringValue(values.get("secondAirline")));
        }
        if (!airlines.isEmpty()) {
            parts.add(String.join("/", airlines));
        }
        append(parts, "bufferHours=" + stringValue(values.get("bufferHours")));
        return String.join(" ", parts).trim();
    }

    private void appendRoute(List<String> parts, Object origin, Object destination) {
        String safeOrigin = stringValue(origin);
        String safeDestination = stringValue(destination);
        if (!safeOrigin.isBlank() && !safeDestination.isBlank()) {
            parts.add(safeOrigin + " -> " + safeDestination);
        }
    }

    private void append(List<String> parts, String value) {
        if (value != null && !value.isBlank() && !value.endsWith("=")) {
            parts.add(value);
        }
    }

    private String joinAny(Object value, String delimiter) {
        if (value instanceof List<?> list) {
            List<String> texts = new ArrayList<>();
            for (Object item : list) {
                String text = stringValue(item);
                if (!text.isBlank()) {
                    texts.add(text);
                }
            }
            return String.join(delimiter, texts);
        }
        return stringValue(value);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String stripMarkdownCodeFence(String raw) {
        String cleaned = raw == null ? "" : raw.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1, cleaned.length() - 3).trim();
            }
        }
        return cleaned;
    }
}

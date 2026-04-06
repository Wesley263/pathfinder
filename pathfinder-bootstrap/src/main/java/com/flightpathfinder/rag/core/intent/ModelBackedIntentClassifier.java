package com.flightpathfinder.rag.core.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.infra.ai.chat.ChatRequest;
import com.flightpathfinder.infra.ai.chat.ChatResponse;
import com.flightpathfinder.infra.ai.chat.ChatService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于模型的意图分类器。
 *
 * 先执行规则分类，再利用模型输出对候选意图做重排与补充。
 */
@Service
@Primary
public class ModelBackedIntentClassifier implements IntentClassifier {

    /** 模型结果最多保留的候选数量。 */
    private static final int MAX_RESULTS = 5;

    /** 规则分类器兜底实现。 */
    private final RuleBasedIntentClassifier fallbackIntentClassifier;
    /** 当前生效的意图树定义。 */
    private final IntentTree intentTree;
    /** 模型调用入口。 */
    private final ChatService chatService;
    /** JSON 解析器，用于解析模型返回的候选结构。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造模型增强版分类器。
     *
     * @param fallbackIntentClassifier 规则兜底分类器
     * @param intentTree 当前意图树
     * @param chatService 模型对话服务
    * @param objectMapper JSON 对象映射器
     */
    public ModelBackedIntentClassifier(RuleBasedIntentClassifier fallbackIntentClassifier,
                                       IntentTree intentTree,
                                       ChatService chatService,
                                       ObjectMapper objectMapper) {
        this.fallbackIntentClassifier = fallbackIntentClassifier;
        this.intentTree = intentTree;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    /**
     * 先拿到规则分类结果，再尝试使用模型进一步排序和增强。
     *
     * @param question 待分类的问题文本
     * @return 融合后的候选意图列表
     */
    @Override
    public List<IntentNodeScore> classifyTargets(String question) {
        List<IntentNodeScore> fallback = fallbackIntentClassifier.classifyTargets(question);
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            return fallback;
        }

        try {
            ChatResponse response = chatService.chat(new ChatRequest(
                    normalizedQuestion,
                    buildSystemPrompt(),
                    Map.of("stage", "intent-classify", "mode", "model-backed")));
            if (response == null || response.placeholder() || response.content() == null || response.content().isBlank()) {
                return fallback;
            }

            List<IntentNodeScore> modelScores = parseScores(response.content());
            if (modelScores.isEmpty()) {
                return fallback;
            }
            return mergeScores(modelScores, fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 构造模型分类提示词。
     *
     * @return 包含意图树说明和输出格式约束的系统提示词
     */
    private String buildSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("You classify one travel question into candidate intents for Flight Pathfinder. ")
                .append("Return JSON array only. Each item must contain id and score. ")
                .append("score must be between 0 and 1. Choose up to ")
                .append(MAX_RESULTS)
                .append(" leaf intents.\n\nAvailable intents:\n");
        for (IntentNode node : intentTree.leafNodes()) {
            builder.append("- id=").append(node.id())
                    .append(" | kind=").append(node.kind())
                    .append(" | name=").append(node.name())
                    .append(" | path=").append(node.fullPath())
                    .append(" | description=").append(node.description())
                    .append(" | keywords=").append(String.join(", ", node.keywords()))
                    .append(" | aliases=").append(String.join(", ", node.aliases()))
                    .append(" | examples=").append(String.join(" / ", node.examples()))
                    .append('\n');
        }
        builder.append("\nOutput example: [{\"id\":\"flight_search\",\"score\":0.92},{\"id\":\"price_lookup\",\"score\":0.55}]");
        return builder.toString();
    }

    /**
     * 解析模型返回的候选列表。
     *
     * @param raw 模型原始输出
     * @return 解析后的候选意图列表
     * @throws Exception 当输出格式非法时抛出异常
     */
    private List<IntentNodeScore> parseScores(String raw) throws Exception {
        String cleaned = stripMarkdownCodeFence(raw);
        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode arrayNode = root;
        if (!root.isArray() && root.has("results")) {
            arrayNode = root.path("results");
        }
        if (!arrayNode.isArray()) {
            return List.of();
        }

        Map<String, IntentNodeScore> scores = new LinkedHashMap<>();
        for (JsonNode item : arrayNode) {
            String id = item.path("id").asText("").trim();
            if (id.isBlank()) {
                continue;
            }
            double score = item.path("score").asDouble(0.0D);
            IntentNode node = intentTree.findLeafNode(id)
                    .or(() -> intentTree.leafNodes().stream().filter(candidate -> candidate.matchesId(id)).findFirst())
                    .orElse(null);
            if (node == null) {
                continue;
            }
            scores.put(node.id(), new IntentNodeScore(node, score));
        }
        return List.copyOf(scores.values());
    }

    /**
     * 融合模型分数与规则分数。
     *
        * 对规则结果保留较低权重，对模型结果赋予较高权重并按分值排序截断。
     *
     * @param modelScores 模型分数
     * @param fallback 规则分数
     * @return 融合后的候选列表
     */
    private List<IntentNodeScore> mergeScores(List<IntentNodeScore> modelScores, List<IntentNodeScore> fallback) {
        Map<String, IntentNodeScore> merged = new LinkedHashMap<>();
        for (IntentNodeScore fallbackScore : fallback) {
            merged.put(fallbackScore.node().id(), new IntentNodeScore(fallbackScore.node(), fallbackScore.score() * 0.25D));
        }
        for (IntentNodeScore modelScore : modelScores) {
            IntentNodeScore previous = merged.get(modelScore.node().id());
            double blendedScore = modelScore.score() * 0.75D + (previous == null ? 0.0D : previous.score());
            merged.put(modelScore.node().id(), new IntentNodeScore(modelScore.node(), blendedScore));
        }
        return merged.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(MAX_RESULTS)
                .toList();
    }

    /**
        * 去除模型输出中可能存在的 Markdown 代码块包裹。
     *
     * @param raw 模型原始输出
        * @return 纯 JSON 文本
     */
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

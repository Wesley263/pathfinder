package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Rule-based classifier for the first 2.0 intent mainline.
 *
 * <p>This implementation intentionally keeps the validated 1.0 KB/MCP/SYSTEM classification
 * idea, while staying independent from retrieval and tool execution.</p>
 */
@Service
public class RuleBasedIntentClassifier implements IntentClassifier {

    private static final String DEFAULT_SYSTEM_INTENT_ID = "general_assistant";
    private static final String PATH_OPTIMIZE_INTENT_ID = "path_optimize";
    private static final String FLIGHT_SEARCH_INTENT_ID = "flight_search";
    private static final String PRICE_LOOKUP_INTENT_ID = "price_lookup";
    private static final String VISA_CHECK_INTENT_ID = "visa_check";
    private static final String CITY_COST_INTENT_ID = "city_cost";
    private static final String RISK_EVALUATE_INTENT_ID = "risk_evaluate";
    private static final double MIN_SCORE = 0.58D;
    private static final int MAX_RESULTS = 5;

    private final IntentTree intentTree;

    public RuleBasedIntentClassifier(IntentTree intentTree) {
        this.intentTree = intentTree;
    }

    /**
     * Ranks leaf intents for one rewritten question.
     *
     * @param question rewritten question or sub-question
     * @return scored candidate intents, or the general system intent when nothing matches
     */
    @Override
    public List<IntentNodeScore> classifyTargets(String question) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.isBlank()) {
            return List.of(defaultSystemIntent());
        }

        List<IntentNodeScore> results = intentTree.leafNodes().stream()
                .map(node -> new IntentNodeScore(node, score(node, normalizedQuestion)))
                .filter(nodeScore -> nodeScore.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(IntentNodeScore::score).reversed())
                .limit(MAX_RESULTS)
                .toList();

        return results.isEmpty() ? List.of(defaultSystemIntent()) : results;
    }

    private double score(IntentNode node, String normalizedQuestion) {
        int keywordHits = countHits(normalizedQuestion, node.keywords());
        int aliasHits = countHits(normalizedQuestion, node.aliases());
        int exampleHits = countHits(normalizedQuestion, node.examples());

        if (keywordHits == 0 && aliasHits == 0 && exampleHits == 0) {
            return 0.0D;
        }

        double score = switch (node.kind()) {
            case MCP -> 0.60D;
            case KB -> 0.58D;
            case SYSTEM -> 0.62D;
        };
        score += Math.min(0.24D, keywordHits * 0.12D);
        score += Math.min(0.10D, aliasHits * 0.05D);
        score += Math.min(0.08D, exampleHits * 0.04D);

        if (PATH_OPTIMIZE_INTENT_ID.equals(node.id()) && hasPathOptimizeSignals(normalizedQuestion)) {
            score += 0.10D;
        }
        if (FLIGHT_SEARCH_INTENT_ID.equals(node.id()) && hasFlightSearchSignals(normalizedQuestion)) {
            score += 0.10D;
        }
        if (PRICE_LOOKUP_INTENT_ID.equals(node.id()) && hasPriceLookupSignals(normalizedQuestion)) {
            score += 0.12D;
        }
        if (VISA_CHECK_INTENT_ID.equals(node.id()) && hasVisaCheckSignals(normalizedQuestion)) {
            score += 0.12D;
        }
        if (CITY_COST_INTENT_ID.equals(node.id()) && hasCityCostSignals(normalizedQuestion)) {
            score += 0.12D;
        }
        if (RISK_EVALUATE_INTENT_ID.equals(node.id()) && hasRiskEvaluateSignals(normalizedQuestion)) {
            score += 0.13D;
        }
        if (FLIGHT_SEARCH_INTENT_ID.equals(node.id()) && hasPriceLookupSignals(normalizedQuestion)) {
            score -= 0.06D;
        }
        if (PRICE_LOOKUP_INTENT_ID.equals(node.id()) && hasCityCostSignals(normalizedQuestion)) {
            score -= 0.08D;
        }
        if (PATH_OPTIMIZE_INTENT_ID.equals(node.id()) && hasRiskEvaluateSignals(normalizedQuestion)) {
            score -= 0.06D;
        }
        if (node.kind() == IntentKind.SYSTEM && looksLikeGreeting(normalizedQuestion)) {
            score += 0.12D;
        }
        return Math.max(0.0D, Math.min(0.99D, score));
    }

    private int countHits(String normalizedQuestion, List<String> candidates) {
        int hits = 0;
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (normalizedQuestion.contains(candidate.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        return hits;
    }

    private boolean hasPathOptimizeSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "预算",
                "中转",
                "转机方案",
                "路径",
                "路线",
                "怎么飞",
                "怎么走",
                "最便宜",
                "省钱",
                "path",
                "route"));
    }

    private boolean hasFlightSearchSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "航班",
                "机票",
                "查航班",
                "搜航班",
                "航班搜索",
                "直飞",
                "票价",
                "出发日期",
                "flight",
                "ticket"));
    }

    private boolean hasPriceLookupSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "比价",
                "价格比较",
                "价格对比",
                "机票比较",
                "哪个便宜",
                "更便宜",
                "最低价",
                "对比价格",
                "比一比",
                "price lookup",
                "price compare",
                "compare price"));
    }

    private boolean hasVisaCheckSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "签证",
                "免签",
                "过境免签",
                "转机免签",
                "入境要求",
                "visa",
                "transit free"));
    }

    private boolean hasCityCostSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "城市成本",
                "生活成本",
                "消费水平",
                "城市开销",
                "日均花费",
                "住宿成本",
                "餐饮成本",
                "交通成本",
                "cost of living",
                "city cost",
                "daily cost"));
    }

    private boolean hasRiskEvaluateSignals(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of(
                "风险评估",
                "中转风险",
                "转机风险",
                "衔接风险",
                "赶得上",
                "稳不稳",
                "误点风险",
                "缓冲时间",
                "risk evaluate",
                "transfer risk",
                "buffer time"));
    }

    private boolean looksLikeGreeting(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of("你好", "您好", "hello", "hi", "你是谁", "讲个笑话", "写首诗"));
    }

    private boolean containsAny(String normalizedQuestion, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedQuestion::contains);
    }

    private IntentNodeScore defaultSystemIntent() {
        IntentNode fallbackNode = intentTree.findLeafNode(DEFAULT_SYSTEM_INTENT_ID)
                .orElseThrow(() -> new IllegalStateException("Missing default system intent node"));
        return new IntentNodeScore(fallbackNode, 0.66D);
    }

    private String normalize(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }
}

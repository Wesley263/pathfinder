package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 基于规则的意图分类器。
 *
 * <p>它延续了 1.0 已验证过的 KB/MCP/SYSTEM 分类思想，但保持在 stage one 内部独立存在，
 * 不与 retrieval 或 MCP 执行链耦合。</p>
 */
@Service
public class RuleBasedIntentClassifier implements IntentClassifier {

    /** 默认 system 兜底意图。 */
    private static final String DEFAULT_SYSTEM_INTENT_ID = "general_assistant";
    /** 路径规划意图标识。 */
    private static final String PATH_OPTIMIZE_INTENT_ID = "path_optimize";
    /** 航班搜索意图标识。 */
    private static final String FLIGHT_SEARCH_INTENT_ID = "flight_search";
    /** 比价意图标识。 */
    private static final String PRICE_LOOKUP_INTENT_ID = "price_lookup";
    /** 签证检查意图标识。 */
    private static final String VISA_CHECK_INTENT_ID = "visa_check";
    /** 城市成本意图标识。 */
    private static final String CITY_COST_INTENT_ID = "city_cost";
    /** 风险评估意图标识。 */
    private static final String RISK_EVALUATE_INTENT_ID = "risk_evaluate";
    /** 候选结果的最低得分阈值。 */
    private static final double MIN_SCORE = 0.58D;
    /** 单次分类最多返回的候选条数。 */
    private static final int MAX_RESULTS = 5;

    /** 当前启用的静态意图树。 */
    private final IntentTree intentTree;

    /**
     * 构造规则分类器。
     *
     * @param intentTree 当前意图树
     */
    public RuleBasedIntentClassifier(IntentTree intentTree) {
        this.intentTree = intentTree;
    }

    /**
     * 对单个问题文本进行规则分类。
     *
     * @param question 改写后的问题或子问题
     * @return 命中的候选意图；若没有有效命中则回落到默认 system 意图
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

    /**
     * 计算单个叶子节点的得分。
     *
     * @param node 当前待评估节点
     * @param normalizedQuestion 已归一化的问题文本
     * @return 节点得分
     */
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

    /**
     * 统计候选词列表命中的数量。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @param candidates 待匹配的候选词
     * @return 命中数量
     */
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

    /**
     * 判断是否存在明显的路径规划信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断是否存在明显的航班搜索信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断是否存在明显的价格比较信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断是否存在明显的签证查询信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断是否存在明显的城市成本信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断是否存在明显的风险评估信号。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 命中则返回 true
     */
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

    /**
     * 判断问题是否更像闲聊或通用问候。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @return 如果更像 system 场景则返回 true
     */
    private boolean looksLikeGreeting(String normalizedQuestion) {
        return containsAny(normalizedQuestion, List.of("你好", "您好", "hello", "hi", "你是谁", "讲个笑话", "写首诗"));
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param normalizedQuestion 已归一化的问题文本
     * @param keywords 关键词集合
     * @return 命中则返回 true
     */
    private boolean containsAny(String normalizedQuestion, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedQuestion::contains);
    }

    /**
     * 返回默认 system 兜底意图。
     *
     * @return 默认 system 意图及其分数
     */
    private IntentNodeScore defaultSystemIntent() {
        IntentNode fallbackNode = intentTree.findLeafNode(DEFAULT_SYSTEM_INTENT_ID)
                .orElseThrow(() -> new IllegalStateException("Missing default system intent node"));
        return new IntentNodeScore(fallbackNode, 0.66D);
    }

    /**
     * 统一问题文本的比较格式。
     *
     * @param question 原始问题
     * @return 归一化后的比较文本
     */
    private String normalize(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }
}

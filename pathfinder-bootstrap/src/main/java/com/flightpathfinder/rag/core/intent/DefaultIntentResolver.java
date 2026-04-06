package com.flightpathfinder.rag.core.intent;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 意图解析器的默认实现。
 *
 * 将改写后的问题拆分为子问题分类结果，并汇总成 KB/MCP/SYSTEM 三路分流。
 */
@Service
public class DefaultIntentResolver implements IntentResolver {

    /** 单类意图汇总后最多保留的候选数。 */
    private static final int MAX_MERGED_PER_KIND = 5;

    /** 单问题意图分类器。 */
    private final IntentClassifier intentClassifier;
    /** MCP 工具注册表，用于补全 MCP 意图的工具显示信息。 */
    private final LocalMcpToolRegistry localMcpToolRegistry;

    /**
     * 构造默认意图解析器。
     *
     * @param intentClassifier 单问题分类器
     * @param localMcpToolRegistry MCP 本地工具注册表
     */
    public DefaultIntentResolver(IntentClassifier intentClassifier, LocalMcpToolRegistry localMcpToolRegistry) {
        this.intentClassifier = intentClassifier;
        this.localMcpToolRegistry = localMcpToolRegistry;
    }

    /**
     * 将改写结果解析成子问题级打分和汇总分流结果。
     *
     * @param rewriteResult 第一阶段的改写输出
     * @return 完整意图解析结果
     */
    @Override
    public IntentResolution resolve(RewriteResult rewriteResult) {
        RewriteResult safeRewriteResult = rewriteResult == null ? new RewriteResult("", List.of(), "", List.of()) : rewriteResult;
        List<String> subQuestions = safeRewriteResult.routingSubQuestions().isEmpty()
                ? List.of(safeRewriteResult.routingQuestion())
                : safeRewriteResult.routingSubQuestions();

        // 子问题逐条分类，保留每条问题的原始候选，便于后续审计与解释。
        List<SubQuestionIntent> subQuestionIntents = subQuestions.stream()
                .map(subQuestion -> new SubQuestionIntent(subQuestion, intentClassifier.classifyTargets(subQuestion)))
                .toList();
        return new IntentResolution(subQuestionIntents, split(subQuestionIntents));
    }

    /**
     * 把所有子问题结果合并成三路分流结果。
     *
     * @param subQuestionIntents 子问题级意图结果列表
     * @return 返回结果。
     */
    private IntentSplitResult split(List<SubQuestionIntent> subQuestionIntents) {
        Map<String, ResolvedIntent> kbIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> mcpIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> systemIntents = new LinkedHashMap<>();

        for (SubQuestionIntent subQuestionIntent : subQuestionIntents) {
            for (IntentNodeScore nodeScore : subQuestionIntent.nodeScores()) {
                ResolvedIntent resolvedIntent = toResolvedIntent(subQuestionIntent.question(), nodeScore);
                // 同类意图只保留最高分，避免一个意图因多个子问题重复占位。
                switch (nodeScore.node().kind()) {
                    case KB -> mergeByHigherScore(kbIntents, resolvedIntent);
                    case MCP -> mergeByHigherScore(mcpIntents, resolvedIntent);
                    case SYSTEM -> mergeByHigherScore(systemIntents, resolvedIntent);
                }
            }
        }

        return new IntentSplitResult(
                limitAndSort(kbIntents),
                limitAndSort(mcpIntents),
                limitAndSort(systemIntents));
    }

    /**
     * 只保留同一意图中得分更高的结果。
     *
     * @param target 目标结果表
     * @param candidate 新候选结果
     */
    private void mergeByHigherScore(Map<String, ResolvedIntent> target, ResolvedIntent candidate) {
        target.merge(candidate.intentId(), candidate,
                (existing, incoming) -> existing.score() >= incoming.score() ? existing : incoming);
    }

    /**
     * 对汇总后的结果按分数排序并做数量裁剪。
     *
     * @param intents 待排序的意图表
     * @return 排序并裁剪后的意图列表
     */
    private List<ResolvedIntent> limitAndSort(Map<String, ResolvedIntent> intents) {
        return intents.values().stream()
                .sorted(Comparator.comparingDouble(ResolvedIntent::score).reversed())
                .limit(MAX_MERGED_PER_KIND)
                .toList();
    }

    /**
        * 将节点打分结果转换为对外统一的已解析意图对象。
     *
     * @param question 命中该意图的子问题
     * @param nodeScore 节点与分数组合
     * @return 已解析意图对象
     */
    private ResolvedIntent toResolvedIntent(String question, IntentNodeScore nodeScore) {
        IntentNode node = nodeScore.node();
        McpToolDescriptor mcpToolDescriptor = node.mcpToolId() == null
                ? null
                : localMcpToolRegistry.findByToolId(node.mcpToolId()).orElse(null);
        return new ResolvedIntent(
                node.id(),
                node.name(),
                node.kind(),
                nodeScore.score(),
                question,
                node.fullPath(),
                node.description(),
                node.mcpToolId(),
                mcpToolDescriptor == null ? null : mcpToolDescriptor.displayName(),
                node.collectionName(),
                node.topK());
    }
}

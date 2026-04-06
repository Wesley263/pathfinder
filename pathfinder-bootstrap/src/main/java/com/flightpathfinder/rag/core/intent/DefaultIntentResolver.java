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
 * <p>它负责把分类器输出的候选意图整合成最终 KB/MCP/SYSTEM 分流结果。这个职责留在 resolver 层，
 * 是因为只有这里能同时看到所有子问题的结果，又不会把分流逻辑泄漏到 rewrite 或 retrieval。</p>
 */
@Service
public class DefaultIntentResolver implements IntentResolver {

    /** 单类意图汇总后最多保留的候选数。 */
    private static final int MAX_MERGED_PER_KIND = 5;

    /** 单问题意图分类器。 */
    private final IntentClassifier intentClassifier;
    /** 本地 MCP 工具注册表，用于补齐已解析意图对应的工具元信息。 */
    private final LocalMcpToolRegistry localMcpToolRegistry;

    /**
     * 构造默认意图解析器。
     *
     * @param intentClassifier 单问题分类器
     * @param localMcpToolRegistry 本地 MCP 工具注册表
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

        // 先保留子问题级分类结果，后续调试和 trace 才能解释“为什么最终走到了这个分支”。
        List<SubQuestionIntent> subQuestionIntents = subQuestions.stream()
                .map(subQuestion -> new SubQuestionIntent(subQuestion, intentClassifier.classifyTargets(subQuestion)))
                .toList();
        return new IntentResolution(subQuestionIntents, split(subQuestionIntents));
    }

    /**
     * 把所有子问题结果合并成三路分流结果。
     *
     * @param subQuestionIntents 子问题级意图结果列表
     * @return 汇总后的 KB/MCP/SYSTEM 分流结果
     */
    private IntentSplitResult split(List<SubQuestionIntent> subQuestionIntents) {
        Map<String, ResolvedIntent> kbIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> mcpIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> systemIntents = new LinkedHashMap<>();

        for (SubQuestionIntent subQuestionIntent : subQuestionIntents) {
            for (IntentNodeScore nodeScore : subQuestionIntent.nodeScores()) {
                ResolvedIntent resolvedIntent = toResolvedIntent(subQuestionIntent.question(), nodeScore);
                // 这里必须显式分三路，避免 retrieval 再次把 KB/MCP 混回一个黑盒路由器。
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
     * 把节点打分转换成 retrieval 可直接消费的已解析意图。
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

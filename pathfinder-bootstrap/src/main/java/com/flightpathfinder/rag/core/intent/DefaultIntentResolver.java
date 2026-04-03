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
 * Default resolver that turns classifier output into KB/MCP/SYSTEM split results.
 *
 * <p>This class owns the final merge because it is the first place that can see all
 * rewritten sub-questions together without leaking split logic into rewrite or retrieval.</p>
 */
@Service
public class DefaultIntentResolver implements IntentResolver {

    private static final int MAX_MERGED_PER_KIND = 5;

    private final IntentClassifier intentClassifier;
    private final LocalMcpToolRegistry localMcpToolRegistry;

    public DefaultIntentResolver(IntentClassifier intentClassifier, LocalMcpToolRegistry localMcpToolRegistry) {
        this.intentClassifier = intentClassifier;
        this.localMcpToolRegistry = localMcpToolRegistry;
    }

    /**
     * Resolves rewritten questions into per-sub-question scores and merged split output.
     *
     * @param rewriteResult rewrite output from stage one
     * @return full intent resolution used by retrieval
     */
    @Override
    public IntentResolution resolve(RewriteResult rewriteResult) {
        RewriteResult safeRewriteResult = rewriteResult == null ? new RewriteResult("", List.of(), "", List.of()) : rewriteResult;
        List<String> subQuestions = safeRewriteResult.routingSubQuestions().isEmpty()
                ? List.of(safeRewriteResult.routingQuestion())
                : safeRewriteResult.routingSubQuestions();

        // Per-sub-question classification is preserved so later debugging can explain why a
        // branch was chosen before everything is merged back into one split result.
        List<SubQuestionIntent> subQuestionIntents = subQuestions.stream()
                .map(subQuestion -> new SubQuestionIntent(subQuestion, intentClassifier.classifyTargets(subQuestion)))
                .toList();
        return new IntentResolution(subQuestionIntents, split(subQuestionIntents));
    }

    private IntentSplitResult split(List<SubQuestionIntent> subQuestionIntents) {
        Map<String, ResolvedIntent> kbIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> mcpIntents = new LinkedHashMap<>();
        Map<String, ResolvedIntent> systemIntents = new LinkedHashMap<>();

        for (SubQuestionIntent subQuestionIntent : subQuestionIntents) {
            for (IntentNodeScore nodeScore : subQuestionIntent.nodeScores()) {
                ResolvedIntent resolvedIntent = toResolvedIntent(subQuestionIntent.question(), nodeScore);
                // The split remains explicit here so retrieval can run KB and MCP as separate
                // branches rather than hiding them behind one black-box router.
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

    private void mergeByHigherScore(Map<String, ResolvedIntent> target, ResolvedIntent candidate) {
        target.merge(candidate.intentId(), candidate,
                (existing, incoming) -> existing.score() >= incoming.score() ? existing : incoming);
    }

    private List<ResolvedIntent> limitAndSort(Map<String, ResolvedIntent> intents) {
        return intents.values().stream()
                .sorted(Comparator.comparingDouble(ResolvedIntent::score).reversed())
                .limit(MAX_MERGED_PER_KIND)
                .toList();
    }

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

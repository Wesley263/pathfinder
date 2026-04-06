package com.flightpathfinder.rag.core.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.mcp.InMemoryLocalMcpToolRegistry;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class DefaultIntentResolverTest {

    @Test
    void resolve_shouldSplitIntentsAndKeepHigherScoreForSameIntent() {
        PathfinderIntentTree intentTree = new PathfinderIntentTree();
        IntentNode flightSearchNode = intentTree.findLeafNode("flight_search").orElseThrow();
        IntentNode priceLookupNode = intentTree.findLeafNode("price_lookup").orElseThrow();
        IntentNode policyKnowledgeNode = intentTree.findLeafNode("policy_knowledge").orElseThrow();
        IntentNode systemNode = intentTree.findLeafNode("general_assistant").orElseThrow();

        IntentClassifier classifier = question -> {
            if (question.contains("第一")) {
                return List.of(
                        new IntentNodeScore(flightSearchNode, 0.70),
                        new IntentNodeScore(policyKnowledgeNode, 0.61));
            }
            if (question.contains("第二")) {
                return List.of(
                        new IntentNodeScore(flightSearchNode, 0.92),
                        new IntentNodeScore(priceLookupNode, 0.88));
            }
            return List.of(new IntentNodeScore(systemNode, 0.66));
        };

        InMemoryLocalMcpToolRegistry toolRegistry = new InMemoryLocalMcpToolRegistry();
        toolRegistry.replaceAll(List.of(new McpToolDescriptor(
                "flight.search",
                "Flight Search Tool",
                "desc",
                Map.of(),
                Map.of())));

        DefaultIntentResolver resolver = new DefaultIntentResolver(classifier, toolRegistry);
        RewriteResult rewriteResult = new RewriteResult(
                "原问题",
                List.of("第一问", "第二问"),
                "内部问题",
                List.of("第一问", "第二问"));

        IntentResolution resolution = resolver.resolve(rewriteResult);

        assertEquals(2, resolution.subQuestionIntents().size());

        List<ResolvedIntent> mcpIntents = resolution.splitResult().mcpIntents();
        assertEquals(2, mcpIntents.size());
        assertEquals("flight_search", mcpIntents.get(0).intentId());
        assertEquals(0.92D, mcpIntents.get(0).score());
        assertEquals("第二问", mcpIntents.get(0).question());
        assertEquals("flight.search", mcpIntents.get(0).mcpToolId());
        assertEquals("Flight Search Tool", mcpIntents.get(0).mcpToolName());

        assertEquals("price_lookup", mcpIntents.get(1).intentId());
        assertNull(mcpIntents.get(1).mcpToolName());

        List<ResolvedIntent> kbIntents = resolution.splitResult().kbIntents();
        assertEquals(1, kbIntents.size());
        assertEquals("policy_knowledge", kbIntents.get(0).intentId());
    }

    @Test
    void resolve_shouldUseRoutingQuestionWhenRoutingSubQuestionsEmpty() {
        PathfinderIntentTree intentTree = new PathfinderIntentTree();
        IntentNode systemNode = intentTree.findLeafNode("general_assistant").orElseThrow();
        List<String> capturedQuestions = new ArrayList<>();
        IntentClassifier classifier = question -> {
            capturedQuestions.add(question);
            return List.of(new IntentNodeScore(systemNode, 0.77));
        };
        DefaultIntentResolver resolver = new DefaultIntentResolver(classifier, new InMemoryLocalMcpToolRegistry());

        RewriteResult rewriteResult = new RewriteResult(
                "展示问题",
                List.of("展示子问题"),
                "内部路由问题",
                List.of());
        IntentResolution resolution = resolver.resolve(rewriteResult);

        assertEquals(List.of("内部路由问题"), capturedQuestions);
        assertEquals(1, resolution.subQuestionIntents().size());
        assertEquals("general_assistant", resolution.splitResult().systemIntents().get(0).intentId());
    }
}


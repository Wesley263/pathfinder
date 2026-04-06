package com.flightpathfinder.rag.core.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class RuleBasedIntentClassifierTest {

    private final RuleBasedIntentClassifier classifier = new RuleBasedIntentClassifier(new PathfinderIntentTree());

    @Test
    void classifyTargets_shouldFallbackToGeneralAssistantForBlankQuestion() {
        List<IntentNodeScore> results = classifier.classifyTargets("   ");

        assertEquals(1, results.size());
        assertEquals("general_assistant", results.get(0).node().id());
        assertEquals(IntentKind.SYSTEM, results.get(0).node().kind());
    }

    @Test
    void classifyTargets_shouldPrioritizeVisaIntent() {
        List<IntentNodeScore> results = classifier.classifyTargets("日本签证要不要办，能不能过境免签？");

        assertFalse(results.isEmpty());
        assertEquals("visa_check", results.get(0).node().id());
        assertEquals(IntentKind.MCP, results.get(0).node().kind());
    }

    @Test
    void classifyTargets_shouldPrioritizeSystemIntentForGreeting() {
        List<IntentNodeScore> results = classifier.classifyTargets("你好，你是谁？");

        assertFalse(results.isEmpty());
        assertEquals("general_assistant", results.get(0).node().id());
        assertEquals(IntentKind.SYSTEM, results.get(0).node().kind());
    }

    @Test
    void classifyTargets_shouldPreferPriceLookupForComparisonQuestion() {
        List<IntentNodeScore> results = classifier.classifyTargets("帮我比价 PVG 到 NRT 和 PVG 到 ICN 哪个更便宜");

        assertFalse(results.isEmpty());
        assertEquals("price_lookup", results.get(0).node().id());
        assertEquals(IntentKind.MCP, results.get(0).node().kind());
    }
}




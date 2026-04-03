package com.flightpathfinder.rag.core.intent;

import java.util.List;

/**
 * Classifies a single rewritten question into candidate intent nodes.
 *
 * <p>This boundary is narrower than the resolver: it only scores candidates and does not
 * decide how results should be merged across multiple sub-questions.</p>
 */
public interface IntentClassifier {

    /**
     * Scores the most relevant intent targets for one question string.
     *
     * @param question rewritten question or sub-question
     * @return ranked candidate leaf intents
     */
    List<IntentNodeScore> classifyTargets(String question);
}

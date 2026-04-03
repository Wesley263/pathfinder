package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record SubQuestionIntent(String question, List<IntentNodeScore> nodeScores) {

    public SubQuestionIntent {
        question = question == null ? "" : question.trim();
        nodeScores = List.copyOf(nodeScores == null ? List.of() : nodeScores);
    }

    public Optional<IntentNodeScore> primaryIntent() {
        return nodeScores.stream().max(Comparator.comparingDouble(IntentNodeScore::score));
    }
}

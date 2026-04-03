package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record IntentResolution(List<SubQuestionIntent> subQuestionIntents, IntentSplitResult splitResult) {

    public IntentResolution {
        subQuestionIntents = List.copyOf(subQuestionIntents == null ? List.of() : subQuestionIntents);
        splitResult = splitResult == null ? IntentSplitResult.empty() : splitResult;
    }

    public static IntentResolution empty() {
        return new IntentResolution(List.of(), IntentSplitResult.empty());
    }

    public Optional<IntentNodeScore> primaryIntent() {
        return subQuestionIntents.stream()
                .flatMap(subQuestionIntent -> subQuestionIntent.nodeScores().stream())
                .max(Comparator.comparingDouble(IntentNodeScore::score));
    }
}
